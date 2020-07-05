package com.atguigu.gmall1213.order.service;

import com.atguigu.gmall1213.model.enums.ProcessStatus;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface OrderService extends IService<OrderInfo> {
    // 保存订单
    Long saveOrderInfo(OrderInfo orderInfo);

    /**
     * 获取流水号 ,将流水号放入缓存
     * @param userId 目的是用userId 在缓存中充当key保存流水号
     * @return
     */
    String getTradeNo(String userId);
    // 比较流水号
    boolean checkTradeNo(String tradeNo,String userId);
    // 删除流水号
    void deleteTradeNo(String userId);

    /**
     * 验证库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(Long skuId, Integer skuNum);

    /**
     * 关闭过期订单
     * @param orderId
     */
    void execExpiredOrder(Long orderId);

    /**
     * 更新订单的方法
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    /**
     * 根据订单Id 查询订单对象
     *  OrderInfo orderInfo = orderService.getById(orderId); 只能单独查询OrderInfo、
     * getOrderInfo 这个方法可以在里面查询订单明细
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(Long orderId);

    /**
     * 发送消息通知库存，减库存！
     * @param orderId
     */
    void sendOrderStatus(Long orderId);


    /**
     * 将orderInfo 转化为map集合
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单方法
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(Long orderId, String wareSkuMap);


    /**
     * 关闭过期订单方法
     * @param orderId
     * @param flag
     */
    void execExpiredOrder(Long orderId, String flag);
}
