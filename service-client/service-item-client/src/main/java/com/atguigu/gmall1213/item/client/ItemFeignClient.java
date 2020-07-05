package com.atguigu.gmall1213.item.client;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.item.client.impl.ItemDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-item",fallback = ItemDegradeFeignClient.class)
public interface ItemFeignClient {

    // 将ItemApiController 中的控制器注入到当前接口

    /**
     * 根据skuId 获取商品详情的数据Result
     * @param skuId
     * @return
     */
    @GetMapping("api/item/{skuId}")
    Result getItem(@PathVariable Long skuId);

}
