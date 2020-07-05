package com.atguigu.gmall1213.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
// import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * @author mqx
 * @date 2020/6/10 11:13
 */
@Configuration // 变成xml
public class CorsConfig {
    // 创建一个Bean 对象
    @Bean
    public CorsWebFilter corsWebFilter(){

        // 创建corsconfiguration();
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 设置跨域的属性
        corsConfiguration.addAllowedOrigin("*"); // 设置允许访问的网络
        corsConfiguration.setAllowCredentials(true); // 表示是否从服务器中获取到cookie ，如果允许true，否则false。
        corsConfiguration.addAllowedMethod("*"); // 表示允许所有的请求方法
        corsConfiguration.addAllowedHeader("*"); //  表示设置请求头信息{设置任意参数}

        // 创建source
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**",corsConfiguration);
        // 返回CorsWebFilter
        return new CorsWebFilter(urlBasedCorsConfigurationSource);
    }
}
