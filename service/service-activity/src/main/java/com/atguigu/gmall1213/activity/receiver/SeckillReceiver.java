package com.atguigu.gmall1213.activity.receiver;

import com.atguigu.gmall1213.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall1213.activity.service.SeckillGoodsService;
import com.atguigu.gmall1213.activity.util.DateUtil;
import com.atguigu.gmall1213.common.config.constant.MqConst;
import com.atguigu.gmall1213.common.constant.RedisConst;
import com.atguigu.gmall1213.model.activity.SeckillGoods;
import com.atguigu.gmall1213.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * @author mqx
 * @date 2020/7/3 10:29
 */
@Component
public class SeckillReceiver{

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    // 监听定时任务发送过来消息
    // 是将数据库中的秒杀商品数据放入缓存！
    // rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_1,"");
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void importData(Message message, Channel channel){
        // 准备查询数据的秒杀商品，将数据放入缓存！
        // 啥是秒杀商品
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        // 审核状态为1 表示审核通过,剩余库存数应该大于0
        seckillGoodsQueryWrapper.eq("status",1).gt("stock_count",0);
        // 查询当天的秒杀商品 start_time 为今天 sql 语句中的格式化
        seckillGoodsQueryWrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillGoods> list = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);
        // 获取到秒杀商品将其放入缓存
        if (!CollectionUtils.isEmpty(list)){
            // 循环遍历
            for (SeckillGoods seckillGoods : list) {
                // 放入缓存的时候，如果缓存中有这个数据，你还需要放入么？ 不需要了。
                // key = seckill:goods
                // hset(key,field,value) key = seckill:goods  field = skuId value = 秒杀商品
                Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).hasKey(seckillGoods.getSkuId().toString());
                // 说明有这个商品
                if (flag){
                    continue;
                }
                // 如果flag=false 说明这个秒杀商品没有在缓存，所以应该将其放入缓存
                redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(),seckillGoods);

                // 存储商品库存的代码
                // 如何控制库存超买？ 将秒杀商品的数量放入到redis - list 这个数据类型中！ lpush ,pop 具有原子性的！
                for (Integer i = 0; i < seckillGoods.getStockCount(); i++) {
                    // 放入数据 lpush key,value
                    // key = seckill:stock:skuId
                    // value = skuId
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId()).leftPush(seckillGoods.getSkuId().toString());
                }
                // 消息发布订阅 channel 表示发送的频道，message 表示发送的内容 skuId:1 表示当前这个商品能够秒杀
                // skuId:0 表示当前商品不能秒杀
                // 商品放入缓存初始化的时候都能秒杀
                // publish seckillpush skuId:1
                redisTemplate.convertAndSend("seckillpush",seckillGoods.getSkuId()+":1");
            }
            // 手动确认接收消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
    }
    // 监听秒杀下单时发送过来的消息
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void seckill(UserRecode userRecode,Message message,Channel channel){
        // 判断
        if(null!=userRecode){
            // 预下单
            seckillGoodsService.seckillOrder(userRecode.getSkuId(),userRecode.getUserId());
            // 消息确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
    }
    // 每天定时清空缓存数据
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_18}
    ))
    public void clearRedisData(Message message,Channel channel){
        // 获取活动结束的商品
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        seckillGoodsQueryWrapper.eq("status",1).le("end_time",new Date());
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);

        // 清空缓存
        for (SeckillGoods seckillGoods : seckillGoodsList) {
            // 商品库存数量
            redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId());
        }
        redisTemplate.delete(RedisConst.SECKILL_GOODS);
        redisTemplate.delete(RedisConst.SECKILL_ORDERS);
        redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);
        // 将审核状态更新一下
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setStatus("2");
        seckillGoodsMapper.update(seckillGoods,seckillGoodsQueryWrapper);

        // 手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
