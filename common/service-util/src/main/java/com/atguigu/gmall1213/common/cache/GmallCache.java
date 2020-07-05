package com.atguigu.gmall1213.common.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author mqx
 * @date 2020/6/16 15:50
 */
@Target(ElementType.METHOD)// 注解在方法上使用
@Retention(RetentionPolicy.RUNTIME) // 这个注解的声明周期是多少
public @interface GmallCache {
    // 表示前缀
    String prefix() default "cache";
}
