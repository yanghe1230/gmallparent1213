package com.atguigu.gmall1213.payment.service;

import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.model.payment.PaymentInfo;

import java.util.Map;

public interface PaymentService {

    // 保存支付记录 数据来源应该是orderInfo
    void savePaymentInfo(String paymentType, OrderInfo orderInfo);

    // 获取交易记录信息
    PaymentInfo getPaymentInfo(String outTradeNo, String name);

    // 支付成功更新交易信息记录
    void paySuccess(String outTradeNo, String name, Map<String, String> paramMap);

    // 更新交易记录
    void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo);

    // 关闭支付宝交易
    void closePayment(Long orderId);
}
