package com.atguigu.gmall1213.activity.client;

import com.atguigu.gmall1213.activity.client.impl.ActivityDegradeFeignClient;
import com.atguigu.gmall1213.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "service-activity" ,fallback = ActivityDegradeFeignClient.class )
public interface ActivityFeignClient {


    /**
     * 返回全部列表
     *
     * @return
     */
    @GetMapping("/api/activity/seckill/findAll")
    Result findAll();

    /**
     * 获取实体
     *
     * @param skuId
     * @return
     */
    @GetMapping("/api/activity/seckill/getSeckillGoods/{skuId}")
    Result getSeckillGoods(@PathVariable Long skuId);

    @GetMapping("/api/activity/seckill/auth/trade")
    Result<Map<String , Object>> trade();
//    Result trade(HttpServletRequest request); HttpServletRequest request 这个可以省略


}
