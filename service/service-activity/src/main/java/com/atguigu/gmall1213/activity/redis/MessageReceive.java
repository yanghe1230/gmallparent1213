package com.atguigu.gmall1213.activity.redis;

import com.atguigu.gmall1213.activity.util.CacheHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class MessageReceive {

    /**接收消息的方法*/
    public void receiveMessage(String message){
        //  publish seckillpush skuId:1
        // 这个message 相当于 skuId:1
        System.out.println("----------收到消息了message："+message);
        if(!StringUtils.isEmpty(message)) {
            /*
             消息格式
                skuId:0 表示没有商品
                skuId:1 表示有商品
             */
            // 将当前的状态位存储到内存中！
            message = message.replaceAll("\"","");
            String[] split = StringUtils.split(message, ":");

            // 只有数据正确的情况下，才会将商品状态位放入内存
//            if (split !=null && split.length == 2) {
//                CacheHelper.put(split[0], split[1]);
//            }
            // 无论你的数据是否正确，都会将状态位放入内存！
            if (split == null || split.length == 2) {
                // CacheHelper 本质就是HashMap map.put(key,value)  split[0] =skuId split[1] =状态位
                CacheHelper.put(split[0], split[1]);
            }

//            if (split.length == 2) {
//                // CacheHelper 本质就是HashMap map.put(key,value)  split[0] =skuId split[1] =状态位
//                CacheHelper.put(split[0], split[1]);
//            }
        }
    }

}
