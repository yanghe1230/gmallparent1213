package com.atguigu.gmall1213.payment.receiver;

import com.atguigu.gmall1213.common.config.constant.MqConst;
import com.atguigu.gmall1213.payment.service.PaymentService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mqx
 * @date 2020/7/1 14:43
 */
@Component
public class PaymentReceiver {

    @Autowired
    private PaymentService paymentService;
    // 开启监听
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_CLOSE,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE),
            key = {MqConst.ROUTING_PAYMENT_CLOSE}
    ))
    public void closePayment(Long orderId , Message message, Channel channel){
        if (null!=orderId){
            // 关闭交易
            paymentService.closePayment(orderId);
        }
        // 手动ack
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
