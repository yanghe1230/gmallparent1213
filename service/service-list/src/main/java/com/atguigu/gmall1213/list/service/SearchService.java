package com.atguigu.gmall1213.list.service;

import com.atguigu.gmall1213.model.list.SearchParam;
import com.atguigu.gmall1213.model.list.SearchResponseVo;

/**
 * @author mqx
 * @date 2020/6/19 11:45
 */
public interface SearchService {

    // 商品上架
    void upperGoods(Long skuId);

    /**
     * 上传多个skuId.
     */
    void upperGoods();
    // 商品下架
    void lowerGoods(Long skuId);

    /**
     * 更新热点
     * @param skuId
     */
    void incrHotScore(Long skuId);

    /**
     * 检索数据
     * @param searchParam
     * @return
     * @throws Exception
     */
    SearchResponseVo search(SearchParam searchParam) throws Exception;
}
