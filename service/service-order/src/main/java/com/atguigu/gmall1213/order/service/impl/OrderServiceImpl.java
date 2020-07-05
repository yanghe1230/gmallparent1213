package com.atguigu.gmall1213.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall1213.common.config.constant.MqConst;
import com.atguigu.gmall1213.common.service.RabbitService;
import com.atguigu.gmall1213.common.util.HttpClientUtil;
import com.atguigu.gmall1213.model.enums.OrderStatus;
import com.atguigu.gmall1213.model.enums.ProcessStatus;
import com.atguigu.gmall1213.model.order.OrderDetail;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.order.mapper.OrderDetailMapper;
import com.atguigu.gmall1213.order.mapper.OrderInfoMapper;
import com.atguigu.gmall1213.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author mqx
 * @date 2020/6/28 9:13
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper,OrderInfo> implements OrderService {

    // 需要注入mapper
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Value("${ware.url}")
    private String WareUrl;


    @Override
    @Transactional
    public Long saveOrderInfo(OrderInfo orderInfo) {
        // 检查页面提交过来的数据，与数据库表中的字段是否完全吻合！
        // 提交数据的时候，缺少的字段数据  总金额，订单状态， 用户Id，订单的交易编号，订单描述，创建时间，过期时间，进度的状态，物流单号{物流系统} ，父订单Id{系统发生拆单的时候}，图片路径{可以忽略}
        // 补充缺少的字段
        orderInfo.sumTotalAmount(); // 计算总金额
        // 用户Id 可以在控制器中获取！暂时先不获取！
        // 订单的交易编号 {对接支付的} 确保不能重复就行了。
        String outTradeNo = "ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        // 订单描述 简单的写，就行 主要为了测试
        // orderInfo.setTradeBody("给我们每个人买礼物");
        // 根据订单明细的中的商品名称进行拼接
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        StringBuffer sb = new StringBuffer();
        for (OrderDetail orderDetail : orderDetailList) {
            sb.append(orderDetail.getSkuName()+" ");
        }
        if (sb.toString().length()>100){
            orderInfo.setTradeBody(sb.toString().substring(0,100));
        }else {
            orderInfo.setTradeBody(sb.toString());
        }
        // 赋值订单状态，初始化时都是未付款
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        // 创建时间
        orderInfo.setCreateTime(new Date());
        // 过期时间 默认一天时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        // 进度的状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());

        // orderInfo
        orderInfoMapper.insert(orderInfo);
        // orderDetail

        if (!CollectionUtils.isEmpty(orderDetailList)){
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setOrderId(orderInfo.getId());
                orderDetailMapper.insert(orderDetail);
            }
        }
        // 保存完成之后发送消息,到了这个过期时间时，取消订单应该根据订单Id 取消。
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,MqConst.ROUTING_ORDER_CANCEL,
                orderInfo.getId(),MqConst.DELAY_TIME);
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        // 定义一个key
        String tradeNoKey = "user:"+userId+":tradeNo";
        // 生产流水号
        String tradeNo = UUID.randomUUID().toString();

        // 将流水号放入缓存
        redisTemplate.opsForValue().set(tradeNoKey,tradeNo);
        return tradeNo;
    }

    /**
     *
     * @param tradeNo 表示页面提交过来的流水号
     * @param userId 从缓存中获取流水的必须使用的key
     * @return
     */
    @Override
    public boolean checkTradeNo(String tradeNo, String userId) {
        // 定义一个key
        String tradeNoKey = "user:"+userId+":tradeNo";
        // 获取到缓存流水号
        String tradeNoRedis = (String) redisTemplate.opsForValue().get(tradeNoKey);
        // 返回比较结果
        return tradeNo.equals(tradeNoRedis);
    }

    @Override
    public void deleteTradeNo(String userId) {
        // 定义一个key
        String tradeNoKey = "user:"+userId+":tradeNo";
        redisTemplate.delete(tradeNoKey);
    }

    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        // http://localhost:9001/hasStock?skuId=xxx&num=xxx
        // WareUrl=http://localhost:9001
        String result = HttpClientUtil.doGet(WareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        // 0 无货， 1 有货
        return "1".equals(result);
    }

    @Override
    public void execExpiredOrder(Long orderId) {
        // 更新数据库中表的状态 order_info
        // update order_info set order_status=CLOSED , process_status=CLOSED where id=orderId
        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        // 发送信息关闭支付宝交易
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
    }

    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        // 创建对象
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        // 订单的状态，可以通过进度状态来获取
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setProcessStatus(processStatus.name());
        orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        // 查询订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(new QueryWrapper<OrderDetail>().eq("order_id", orderId));
        orderInfo.setOrderDetailList(orderDetailList);
        // 返回数据
        return orderInfo;
    }

    @Override
    public void sendOrderStatus(Long orderId) {
        // 更改订单的状态，变成通知仓库准备发货
        updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
        // 需要参考库存管理文档 根据管理手册。
        // 发送的数据 是 orderInfo 中的部分属性数据，并非全部属性数据！
        // 获取发送的字符串：
        String wareJson = initWareOrder(orderId);
        // 准备发送消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK,MqConst.ROUTING_WARE_STOCK,wareJson);
    }

    public String initWareOrder(Long orderId) {
        // 首先查询到orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);
        // 将orderInfo 中的部分属性，放入一个map 集合中。
        Map map = initWareOrder(orderInfo);
        // 返回json 字符串
        return JSON.toJSONString(map);
    }


    // 将orderInfo 部分数据组成map
    public Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());// 仓库Id ，减库存拆单时需要使用！
        /*
            details 对应的是订单明细
            details:[{skuId:101,skuNum:1,skuName:’小米手64G’},
                       {skuId:201,skuNum:1,skuName:’索尼耳机’}]
         */
        // 声明一个list 集合 来存储map
        List<Map> maps = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            // 先声明一个map 集合
            HashMap<String, Object> orderDetailMap = new HashMap<>();
            orderDetailMap.put("skuId",orderDetail.getSkuId());
            orderDetailMap.put("skuNum",orderDetail.getSkuNum());
            orderDetailMap.put("skuName",orderDetail.getSkuName());
            maps.add(orderDetailMap);
        }

        // map.put("details", JSON.toJSONString(maps));
        map.put("details", maps);
        // 返回构成好的map集合。
        return map;
    }

    /**
     * 获取子订单集合
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    @Override
    public List<OrderInfo> orderSplit(Long orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        /*
        1.  先获取原始订单
        2.  wareSkuMap [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
            将上述数据变为java 代码能够识别的对象
        3.  创建新的子订单
        4.  给子订单进行赋值
        5.  保存子订单
        6.  更新原始订单的状态
        7.  test
         */
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        // wareSkuMap 编程集合map
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        // 子订单根据什么来创建
        for (Map map : mapList) {
            // 获取map 中的仓库Id
            String wareId = (String) map.get("wareId");
            // 获取仓库Id 对应的商品 Id
            List<String> skuIdList = (List<String>) map.get("skuIds");
            OrderInfo subOrderInfo = new OrderInfo();
            // 属性拷贝，原始订单的基本数据，都可以给子订单使用
            BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
            // id 不能拷贝，发送主键冲突
            subOrderInfo.setId(null);
            subOrderInfo.setParentOrderId(orderId);
            // 赋值一个仓库Id
            subOrderInfo.setWareId(wareId);
            // 计算总金额 在订单的实体类中有sumTotalAmount() 方法。
            // 声明一个子订单明细集合
            List<OrderDetail> orderDetails = new ArrayList<>();

            // 需要将子订单的名单明细准备好,添加到子订单中
            // 子订单明细应该来自于原始订单明细。
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            if (!CollectionUtils.isEmpty(orderDetailList)){
                // 遍历原始的订单明细
                for (OrderDetail orderDetail : orderDetailList) {
                    // 再去遍历仓库中所对应的商品Id
                    for (String skuId : skuIdList) {
                        // 比较两个商品skuId ，如果相同，则这个商品就是子订单明细需要的商品
                        if (Long.parseLong(skuId)==orderDetail.getSkuId()){
                            orderDetails.add(orderDetail);
                        }
                    }
                }
            }
            // 需要将子订单的名单明细准备好,添加到子订单中
            subOrderInfo.setOrderDetailList(orderDetails);
            // 获取到总金额
            subOrderInfo.sumTotalAmount();
            // 保存子订单
            saveOrderInfo(subOrderInfo);

            // 将新的子订单放入集合中
            subOrderInfoList.add(subOrderInfo);
        }
        // 更新原始订单的状态
        updateOrderStatus(orderId,ProcessStatus.SPLIT);
        return subOrderInfoList;
    }

    @Override
    public void execExpiredOrder(Long orderId, String flag) {
        // 更新订单状态状态
        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        if ("2".equals(flag)){
            // 发送信息关闭支付宝交易
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
        }
    }

}
