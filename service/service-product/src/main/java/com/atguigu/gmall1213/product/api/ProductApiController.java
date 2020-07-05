package com.atguigu.gmall1213.product.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.product.*;
import com.atguigu.gmall1213.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author mqx
 * 整个类中的数据，都是为service-item 服务提供的
 * @date 2020/6/13 11:49
 */
@RestController
@RequestMapping("api/product")
public class ProductApiController {

    @Autowired
    private ManageService manageService;
    // 这个控制器中写谁？ 自己内部做一个规定
    // 根据skuId 获取skuInfo,skuImage
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfoById(@PathVariable Long skuId){
        return manageService.getSkuInfo(skuId);
    }

    // 根据三级分类Id 查询分类名称
    @GetMapping("inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id){
        return manageService.getBaseCategoryViewBycategory3Id(category3Id);
    }

    // 根据skuId 获取商品价格
    @GetMapping("inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId){
        return manageService.getSkuPriceBySkuId(skuId);
    }

    // 回显销售属性-销售属性值
    @GetMapping("inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                          @PathVariable Long spuId){
        return manageService.getSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    // 点击销售属性值进行切换
    @GetMapping("inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId){
        return manageService.getSkuValueIdsMap(spuId);
    }

    // 首页显示三级分类
    @GetMapping("getBaseCategoryList")
    public Result getBaseCategoryList(){
        List<JSONObject> baseCategoryList = manageService.getBaseCategoryList();

        return Result.ok(baseCategoryList);
    }

    // 品牌信息
    @GetMapping("inner/getTrademark/{tmId}")
    public BaseTrademark getTrademarkByTmId(@PathVariable Long tmId){
        return manageService.getBaseTrademarkByTmId(tmId);
    }

    // 平台属性信息
    @GetMapping("inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId){
        List<BaseAttrInfo> attrInfoList = manageService.getAttrInfoList(skuId);
        return attrInfoList;
    }




//    public static void main(String[] args) throws ExecutionException, InterruptedException {
//
//        // 定义一个线程池
//        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
//                50,
//                500,
//                30,
//                TimeUnit.SECONDS,
//                new ArrayBlockingQueue<>(100));
//
//        // 创建一个异步编排对象
//        // Supplier
//        CompletableFuture<String> futureA = CompletableFuture.supplyAsync(() -> {
//            return "hello";
//        });
//
//        //futureB
//        //异步操作：不在当前线程内执行
//        //  Consumer
//        CompletableFuture<Void> futureB = futureA.thenAcceptAsync((s) -> {
//            //让当前线程进行一个睡眠  1 秒钟
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            System.out.println(s + "\tfutureB");
//        },threadPoolExecutor);
//
//        CompletableFuture<Void> futureC = futureA.thenAcceptAsync((s) -> {
//            //让当前线程进行一个睡眠 3 秒钟
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            System.out.println(s + "\tfutureC");
//        },threadPoolExecutor);
//
//        // 同时都是引用的futureA
//        System.out.println(futureB.get());
//        System.out.println(futureC.get());


        //创建异步编排对象
        //使用lamd 表达式来完成
        //Supplier
//        CompletableFuture.supplyAsync(()->{
//
//        })
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//            @Override
//            public Integer get() {
//                System.out.println(Thread.currentThread().getName() + "haha,nilaile");
//                //int i = 1/0;
//                return 666;
////                return 200;
//
//            }
//        }).thenApply(new Function<Integer, Integer>() {//依赖上一个执行结果
//            @Override
//            public Integer apply(Integer integer) {
//                System.out.println("执行这个thenApply----"+integer);
//                return integer*2;
//            }
//        })
//                .whenComplete(new BiConsumer<Integer, Throwable>() {
//            @Override
//            public void accept(Integer o, Throwable throwable) {
//                System.out.println("o===="+o);
//                System.out.println("throwable==="+throwable);
//            }
//        }).exceptionally(new Function<Throwable, Integer>() {
//            @Override
//            public Integer apply(Throwable throwable) {
//                System.out.println("throwable=--------=="+throwable);
//                return 8888;
//            }
//        });
//
//        //调用get() 方法
//        System.out.println(future.get());
//    }



}
