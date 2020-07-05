package com.atguigu.gmall1213.product.service;

/**
 * @author mqx
 * @date 2020/6/15 15:15
 */
public interface TestService {

    // 测试锁
    void testLock();

    String readLock();

    String writeLock();
}
