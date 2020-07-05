package com.atguigu.gmall1213.product.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.product.SkuInfo;
import com.atguigu.gmall1213.model.product.SpuImage;
import com.atguigu.gmall1213.model.product.SpuSaleAttr;
import com.atguigu.gmall1213.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author mqx
 * http://api.gmall.com/admin/product/spuImageList/{spuId}
 * @date 2020/6/12 11:31
 */
@RestController
@RequestMapping("admin/product")
public class SkuManageController {
    // 调用服务层
    @Autowired
    private ManageService manageService;

    @GetMapping("spuImageList/{spuId}")
    public Result getSpuImageList(@PathVariable Long spuId){
        List<SpuImage> spuImageList = manageService.getSpuImageList(spuId);
        // 返回图片列表
        return Result.ok(spuImageList);
    }

    // 回显销售属性，属性值控制器
    // http://api.gmall.com/admin/product/spuSaleAttrList/{spuId}
    @GetMapping("spuSaleAttrList/{spuId}")
    public Result spuSaleAttrList(@PathVariable Long spuId){
        // 多个销售属性{ 销售属性中有消息属性值集合}
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrList(spuId);

        // 返回数据
        return Result.ok(spuSaleAttrList);

    }
    // 大保存skuInfo数据
    // http://api.gmall.com/admin/product/saveSkuInfo POST
    // 获取 页面传递过来的数据 Json ---> JavaObject
    @PostMapping("saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        // 调用服务层
        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }
    // http://api.gmall.com/admin/product/list/1/10
    @GetMapping("list/{page}/{limit}")
    public Result getList(@PathVariable Long page,
                          @PathVariable Long limit){

        // 需要将page,limit 传给Page 对象。
        Page<SkuInfo> skuInfoPage = new Page<>(page,limit);
        IPage<SkuInfo> skuInfoIPage = manageService.selectPage(skuInfoPage);
        // 返回skuInfo 列表数据
        return Result.ok(skuInfoIPage);

    }

    // 商品的上架
    // http://api.gmall.com/admin/product/onSale/34
    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        // 调用服务层
        manageService.onSale(skuId);

        return Result.ok();
    }

    // http://api.gmall.com/admin/product/cancelSale/33
    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        // 调用服务层
        manageService.cancelSale(skuId);

        return Result.ok();
    }


}
