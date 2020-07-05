package com.atguigu.gmall1213.list.receiver;

import com.atguigu.gmall1213.common.config.constant.MqConst;
import com.atguigu.gmall1213.list.service.SearchService;
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
 * @date 2020/6/29 9:31
 */
@Component
public class ListReceiver {

    @Autowired
    private SearchService searchService;

    // 使用注解来监听消息

    /**
     * 商品的上架
     * @param skuId
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    public void upperGoods(Long skuId, Message message, Channel channel){
        // 判断skuId
        if (null!=skuId){
            // 商品上架
            searchService.upperGoods(skuId);
        }
        // 手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    /**
     * 商品的下架
     * @param skuId
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_LOWER}
    ))
    public void lowerGoods(Long skuId, Message message, Channel channel){
        // 判断skuId
        if (null!=skuId){
            // 商品下架
            searchService.lowerGoods(skuId);
        }
        // 手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
