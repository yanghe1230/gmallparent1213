package com.atguigu.gmall1213.activity.client.impl;

import com.atguigu.gmall1213.activity.client.ActivityFeignClient;
import com.atguigu.gmall1213.common.result.Result;
import org.springframework.stereotype.Component;

@Component
public class ActivityDegradeFeignClient implements ActivityFeignClient {
    @Override
    public Result findAll() {
        return null;
    }

    @Override
    public Result getSeckillGoods(Long skuId) {
        return null;
    }
}
