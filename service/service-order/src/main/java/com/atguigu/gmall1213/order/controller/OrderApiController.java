package com.atguigu.gmall1213.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall1213.cart.client.CartFeignClient;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.common.util.AuthContextHolder;
import com.atguigu.gmall1213.model.cart.CartInfo;
import com.atguigu.gmall1213.model.order.OrderDetail;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.model.user.UserAddress;
import com.atguigu.gmall1213.order.service.OrderService;
import com.atguigu.gmall1213.product.client.ProductFeignClient;
import com.atguigu.gmall1213.user.client.UserFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author mqx
 * @date 2020/6/24 15:21
 */
@RestController
@RequestMapping("api/order")
public class OrderApiController {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    @Autowired
    private OrderService orderService;


    // 订单 在网关中设置过这个拦截 /api/**/auth/** 必须登录才能访问
    @GetMapping("auth/trade")
    public Result<Map<String,Object>> trade(HttpServletRequest request){
        // 登录之后的用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 获取用户地址列表 根据用户Id
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);

        // 获取送货清单
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);

        // 声明一个OrderDetail 集合
        List<OrderDetail> orderDetailList = new ArrayList<>();

        int totalNum = 0;
        // 循环遍历，将数据赋值给orderDetail
        if (!CollectionUtils.isEmpty(cartCheckedList)){
            // 循环遍历
            for (CartInfo cartInfo : cartCheckedList) {
                // 将cartInfo 赋值给 orderDetail
                OrderDetail orderDetail = new OrderDetail();

                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setOrderPrice(cartInfo.getSkuPrice());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setSkuId(cartInfo.getSkuId());
                // 计算每个商品的总个数。
                totalNum+= cartInfo.getSkuNum();
                // 将每一个orderDeatil 添加到集合中
                orderDetailList.add(orderDetail);
            }
        }

        // 获取流水号
        String tradeNo = orderService.getTradeNo(userId);
        // 声明一个map 集合来存储数据
        Map<String , Object> map = new HashMap<>();
        // 存储订单明细
        map.put("detailArrayList",orderDetailList);
        // 存储收货地址列表
        map.put("userAddressList",userAddressList);
        // 存储总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        // 计算总金额
        orderInfo.sumTotalAmount();
        map.put("totalAmount",orderInfo.getTotalAmount());
        // 存储商品的件数 记录大的商品有多少个
        map.put("totalNum",orderDetailList.size());

        // 保存tradeNo
        map.put("tradeNo",tradeNo);
        // 计算小件数：
        // map.put("totalNum",totalNum);
        return Result.ok(map);
    }

    // 提交订单：
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        // 用户Id
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));

        // 防止表单回退无刷新提交
        // 在后台能够获取页面提交过来的流水号
        // http://api.gmall.com/api/order/auth/submitOrder?tradeNo=null
        String tradeNo = request.getParameter("tradeNo");
        // 开始比较
        boolean flag = orderService.checkTradeNo(tradeNo, userId);
        // 如果返回的是true ，则说明第一次提交，如果是false 说明无刷新重复提交了！
//        if (flag){
//            // 正常
//        }else {
//            // 异常
//        }
        // 异常情况
        if (!flag){
            // 如果是false 说明无刷新重复提交了！
            return Result.fail().message("不能回退无刷新重复提交订单!");
        }
        // 创建一个集合对象，来存储异常信息
        List<String> errorList = new ArrayList<>();
        // 使用异步编排来执行
        // 声明一个集合来存储异步编排对象
        List<CompletableFuture> futureList = new ArrayList<>();
        // 验证库存：验证每个商品，存在orderDetailList
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)){
            for (OrderDetail orderDetail : orderDetailList) {
                // 开一个异步编排
                CompletableFuture<Void> checkStockCompletableFuture = CompletableFuture.runAsync(() -> {
                    // 调用查询库存方法
                    boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                    if (!result) {
                        // 提示信息某某商品库存不足
                        // return Result.fail().message(orderDetail.getSkuName()+"库存不足！");
                        errorList.add(orderDetail.getSkuName() + "库存不足！");
                    }
                }, threadPoolExecutor);
                // 将验证库存的异步编排对象放入这个集合
                futureList.add(checkStockCompletableFuture);

                // 利用另一个异步编排来验证价格
                CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
                    // 获取到商品的实时价格
                    BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                    // 判断 价格有变化，要么大于 1 ，要么小于 -1。说白了 ,相等 0
                    if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {
                        // 如果价格有变动，则重新查询。
                        // 订单的价格来自于购物车，只需要将购物车的价格更改了，重新下单就可以了。
                        cartFeignClient.loadCartCache(userId);
                        //return Result.fail().message(orderDetail.getSkuName()+"价格有变动,请重新下单！");
                        errorList.add(orderDetail.getSkuName()+"价格有变动,请重新下单！");
                    }
                }, threadPoolExecutor);
                // 将验证价格的异步编排添加到集合中
                futureList.add(skuPriceCompletableFuture);
            }
        }
        // 合并线程 所有的异步编排都在futureList
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()])).join();
        // 返回页面提示信息
        if (errorList.size()>0){
            // 获取异常集合的数据
            return Result.fail().message(StringUtils.join(errorList,","));
        }

//        if (!CollectionUtils.isEmpty(orderDetailList)){
//            for (OrderDetail orderDetail : orderDetailList) {
//                // 调用查询库存方法
//                boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
//                if (!result){
//                    // 提示信息某某商品库存不足
//                    return Result.fail().message(orderDetail.getSkuName()+"库存不足！");
//                }
//
//                // 验证价格：
//                // 获取到商品的实时价格
//                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
//                // 判断 价格有变化，要么大于 1 ，要么小于 -1。说白了 ,相等 0
//                if (orderDetail.getOrderPrice().compareTo(skuPrice)!=0){
//                    // 如果价格有变动，则重新查询。
//                    // 订单的价格来自于购物车，只需要将购物车的价格更改了，重新下单就可以了。
//                    cartFeignClient.loadCartCache(userId);
//                    return Result.fail().message(orderDetail.getSkuName()+"价格有变动,请重新下单！");
//                }
//            }
//        }
        // 删除流水号
        orderService.deleteTradeNo(userId);

        Long orderId = orderService.saveOrderInfo(orderInfo);
        // 返回用户Id
        return Result.ok(orderId);
    }

    // 根据订单Id 查询订单数据对象
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId){
        return orderService.getOrderInfo(orderId);
    }

    // http://order.gmall.com/orderSplit?orderId=xxx&wareSkuMap=xxx
    @RequestMapping("orderSplit")
    public String orderSplit(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        // 仓库编号与商品的对照关系
        String wareSkuMap = request.getParameter("wareSkuMap");

        // 参考拆单接口文档 获取子订单集合
        List<OrderInfo> subOrderInfoList =  orderService.orderSplit(Long.parseLong(orderId),wareSkuMap);

        // 声明一个存储map的集合
        List<Map> mapList = new ArrayList<>();

        // 将子订单中的部分数据转换为json 字符串
        for (OrderInfo orderInfo : subOrderInfoList) {
            // 将部分数据转换为map
            Map map = orderService.initWareOrder(orderInfo);
            mapList.add(map);
        }
        // 返回子订单的json 字符串
        return JSON.toJSONString(mapList);
    }

    // 封装秒杀订单数据 控制器可以从页面获取到！
    // 将前台获取到json 字符串变为java 对象
    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo){
        // 调用保存订单方法
        Long orderId = orderService.saveOrderInfo(orderInfo);
        // 返回订单Id
        return orderId;
    }

}
