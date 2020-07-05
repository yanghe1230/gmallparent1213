package com.atguigu.gmall1213.payment.service.impl;

import com.atguigu.gmall1213.common.config.constant.MqConst;
import com.atguigu.gmall1213.common.service.RabbitService;
import com.atguigu.gmall1213.model.enums.PaymentStatus;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.model.payment.PaymentInfo;
import com.atguigu.gmall1213.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall1213.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * @author mqx
 * @date 2020/6/29 16:23
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public void savePaymentInfo(String paymentType, OrderInfo orderInfo) {

        // 交易记录中如果有当前对应的订单Id 时，那么还能否继续插入当前数据。
        QueryWrapper<PaymentInfo> orderInfoQueryWrapper = new QueryWrapper<>();
        orderInfoQueryWrapper.eq("order_id",orderInfo.getId());
        orderInfoQueryWrapper.eq("payment_type",paymentType);

        Integer count = paymentInfoMapper.selectCount(orderInfoQueryWrapper);
        if (count>0){
            return;
        }
        // 创建一个对象
        PaymentInfo paymentInfo = new PaymentInfo();

        // 给对象赋值
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());

        paymentInfoMapper.insert(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String name) {

        // 根据out_trade_no 以及支付方式查询交易记录
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo).eq("payment_type",name);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        return paymentInfo;
    }

    @Override
    public void paySuccess(String outTradeNo, String name, Map<String, String> paramMap) {
        // 需要获取到订单Id
        PaymentInfo paymentInfo = this.getPaymentInfo(outTradeNo, name);
        // 如果当前订单交易记录 已经是付款完成的，或者是交易关闭的。则后续业务不会执行！
        if (paymentInfo.getPaymentStatus().equals(PaymentStatus.PAID.name())
                || paymentInfo.getPaymentStatus().equals(PaymentStatus.ClOSED.name())){
            return;
        }

        // 第一个参数更新的内容，第二个参数更新的条件
        PaymentInfo paymentInfoUPD = new PaymentInfo();
        paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfoUPD.setCallbackTime(new Date());
        // 更新支付宝的交易号，交易号在map 中
        paymentInfoUPD.setTradeNo(paramMap.get("trade_no"));
        paymentInfoUPD.setCallbackContent(paramMap.toString());

        // 构造更新条件
        // update payment_info set trade_no = ？，payment_status=？ ... where out_trade_no = outTradeNo and payment_type = name
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo).eq("payment_type",name);
        paymentInfoMapper.update(paymentInfoUPD,paymentInfoQueryWrapper);

        // 发送消息通知订单
        // 更新订单状态 订单Id 或者 outTradeNo
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfo.getOrderId());
    }

    @Override
    public void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo) {
        // 根据第三方交易编号更新交易记录。
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo);
        paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);
    }

    @Override
    public void closePayment(Long orderId) {

        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<PaymentInfo>();
        queryWrapper.eq("order_id", orderId);
        // 关闭交易
        // 先查询 paymentInfo 交易记录 select count(*) from payment_info where order_id = orderId
        Integer count = paymentInfoMapper.selectCount(queryWrapper);
        if (null==count || count.intValue()==0 ){
            // 说明这个订单没有交易记录，
            return;
        }
        // 否则要关闭
        // update payment_info set PaymentStatus = CLOSED where order_id = orderId
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
        paymentInfoMapper.update(paymentInfo,queryWrapper);

    }
}
