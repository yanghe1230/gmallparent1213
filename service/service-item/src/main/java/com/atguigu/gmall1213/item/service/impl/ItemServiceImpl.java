package com.atguigu.gmall1213.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall1213.item.service.ItemService;
import com.atguigu.gmall1213.list.client.ListFeignClient;
import com.atguigu.gmall1213.model.product.BaseCategoryView;
import com.atguigu.gmall1213.model.product.SkuInfo;
import com.atguigu.gmall1213.model.product.SpuSaleAttr;
import com.atguigu.gmall1213.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author mqx
 * @date 2020/6/13 11:32
 */
@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;

    // 编写一个自定义的线程池！
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ListFeignClient listFeignClient;

    @Override
    public Map<String, Object> getBySkuId(Long skuId) {
        Map<String, Object> result = new HashMap<>();

        // 异步编排  通过skuId 获取skuInfo 对象数据 ，这个skuInfo 在后面会使用到其中的属性
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
            result.put("skuInfo", skuInfo);
            return skuInfo;
        },threadPoolExecutor);
        // 查询销售属性，销售属性值的时候，返回来的集合只需要保存到map中，并没有任何方法，需要这个集合数据作为参数传递。
        // 销售属性值，销售属性值的时候 需要skuInfo对象中的getSpuId 所以此处应该使用skuInfoCompletableFuture！
        // 不使用，supplyAsync runAsync.没有该方法 ，
//        Consumerbase_category_view
//        idea 默认写法 也可以实现！ 复制小括号，写死右箭头，落地大括号
        CompletableFuture<Void> spuSaleAttrCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo -> {
            List<SpuSaleAttr> spuSaleAttrListCheckBySku = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            // 保存到map 集合
            result.put("spuSaleAttrList", spuSaleAttrListCheckBySku);
        }),threadPoolExecutor);
//        CompletableFuture<Void> spuSaleAttrCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo) -> {
//            List<SpuSaleAttr> spuSaleAttrListCheckBySku = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
////            // 保存到map 集合
//            result.put("spuSaleAttrList", spuSaleAttrListCheckBySku);
//        },threadPoolExecutor);

        // 查询分类数据，需要skuInfo的三级分类Id
//        Consumer
        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo) -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            // 保存三级分类数据
            result.put("categoryView", categoryView);
        },threadPoolExecutor);

        // 通过skuId 获取价格数据 runAsync 不需要返回值！
        // 方法一：
        CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            // 保存商品价格
            result.put("price", skuPrice);
        },threadPoolExecutor);
//        方法二
//        skuInfoCompletableFuture.thenAcceptAsync((skuInfo -> {
//            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuInfo.getId());
//            // 保存商品价格
//            result.put("price", skuPrice);
//        }));

        // 根据spuId 获取 由销售属性值Id 和skuId 组成的map 集合数据 ,第二个参数是一个线程池。如果不写，程序会有一个默认的线程池。
        CompletableFuture<Void> valuesSkuJsonCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo -> {
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
//             需要将skuValueIdsMap 转化为Json 字符串，给页面使用!  Map --->Json
            String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
            // 保存销售属性值Id 和 skuId 组成的json 字符串
            result.put("valuesSkuJson",valuesSkuJson);
        }),threadPoolExecutor);
        // 热度排名计算！
        /*
        @GetMapping("api/list/inner/incrHotScore/{skuId}")
        Result incrHotScore(@PathVariable Long skuId);
        方式一：skuId = skuInfo.getId();
        方式二：可以直接获取传递过来的参数 skuId
         */
        // CompletableFuture.supplyAsync() // 有返回值 1
        // 没有返回值 2
        CompletableFuture<Void> incrHotScoreCompletableFuture = CompletableFuture.runAsync(() -> {
            // 远程调用热度排名方法
            listFeignClient.incrHotScore(skuId);
        }, threadPoolExecutor);

        // 将所有的异步编排做整合
        CompletableFuture.allOf(skuInfoCompletableFuture,
                spuSaleAttrCompletableFuture,
                categoryViewCompletableFuture,
                priceCompletableFuture,
                valuesSkuJsonCompletableFuture,
                incrHotScoreCompletableFuture).join();
        return result;
    }
}

