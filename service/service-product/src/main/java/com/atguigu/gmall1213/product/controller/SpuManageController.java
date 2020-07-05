package com.atguigu.gmall1213.product.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.product.BaseSaleAttr;
import com.atguigu.gmall1213.model.product.SpuInfo;
import com.atguigu.gmall1213.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author mqx
 * http://api.gmall.com/admin/product/baseSaleAttrList
 * @date 2020/6/12 9:03
 */
@RestController
@RequestMapping("admin/product")
public class SpuManageController {

    @Autowired
    private ManageService manageService;

    @GetMapping("baseSaleAttrList")
    public Result baseSaleAttrList(){
       List<BaseSaleAttr> baseSaleAttrList = manageService.getBaseSaleAttrList();

       // 将数据返回
        return Result.ok(baseSaleAttrList);

    }

    // http://api.gmall.com/admin/product/saveSpuInfo POST
    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
        if (null!=spuInfo){
            // 调用服务层
            manageService.saveSpuInfo(spuInfo);
        }
        return Result.ok(); // 200 成功
    }



}
