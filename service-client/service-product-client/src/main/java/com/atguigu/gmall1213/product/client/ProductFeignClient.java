package com.atguigu.gmall1213.product.client;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.product.*;
import com.atguigu.gmall1213.product.client.impl.ProductDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(name = "service-product",fallback = ProductDegradeFeignClient.class)
public interface ProductFeignClient {

    // 这个是远程调用接口 {service-product 这个微服务 中内部接口 ProductApiController}
    // ProductApiController 控制器中的数据应该给service-item 使用。

    /**
     * 表示根据skuId 获取skuInfo 对象
     * @param skuId
     * @return
     */
    @GetMapping("api/product/inner/getSkuInfo/{skuId}")
    SkuInfo getSkuInfoById(@PathVariable Long skuId);

    /**
     * 根据三级分类Id 查询分类数据
     * @param category3Id
     * @return
     */
    @GetMapping("api/product/inner/getCategoryView/{category3Id}")
    BaseCategoryView getCategoryView(@PathVariable Long category3Id);

    /**
     * 根据skuId 查询价格信息
     * @param skuId
     * @return
     */
    @GetMapping("api/product/inner/getSkuPrice/{skuId}")
    BigDecimal getSkuPrice(@PathVariable Long skuId);

    /**
     * 根据skuId,spuId 查询销售属性-销售属性值
     * @param skuId
     * @param spuId
     * @return
     */
    @GetMapping("api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId, @PathVariable Long spuId);

    /**
     * 根据spuId 获取销售属性切换数据
     * @param spuId
     * @return
     */
    @GetMapping("api/product/inner/getSkuValueIdsMap/{spuId}")
    Map getSkuValueIdsMap(@PathVariable Long spuId);

    /**
     * 首页数据
     * @return
     */
    @GetMapping("/api/product/getBaseCategoryList")
    Result getBaseCategoryList();

    /**
     * 根据品牌Id 查询数据
     * @param tmId
     * @return
     */
    @GetMapping("api/product/inner/getTrademark/{tmId}")
    BaseTrademark getTrademarkByTmId(@PathVariable Long tmId);

    /**
     * 根据skuId 查询平台属性，平台属性值
     * @param skuId
     * @return
     */
    @GetMapping("api/product/inner/getAttrList/{skuId}")
    List<BaseAttrInfo> getAttrList(@PathVariable Long skuId);
}
