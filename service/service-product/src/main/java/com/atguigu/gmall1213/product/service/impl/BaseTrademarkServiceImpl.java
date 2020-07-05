package com.atguigu.gmall1213.product.service.impl;

import com.atguigu.gmall1213.model.product.BaseTrademark;
import com.atguigu.gmall1213.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall1213.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author mqx
 * @date 2020/6/10 14:42
 */
@Service
public class BaseTrademarkServiceImpl extends ServiceImpl<BaseTrademarkMapper,BaseTrademark> implements BaseTrademarkService {

    // 调用mapper
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Override
    public IPage<BaseTrademark> selectPage(Page<BaseTrademark> baseTrademarkPage) {

        // 定义查询规则 做一个按照Id升序排列
        QueryWrapper<BaseTrademark> baseTrademarkQueryWrapper = new QueryWrapper<>();
        baseTrademarkQueryWrapper.orderByAsc("id");
        return baseTrademarkMapper.selectPage(baseTrademarkPage,baseTrademarkQueryWrapper);

    }
}
