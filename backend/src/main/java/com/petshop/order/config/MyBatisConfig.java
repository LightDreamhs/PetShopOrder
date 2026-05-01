package com.petshop.order.config;

import com.petshop.order.interceptor.AutoFillInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisConfig {

    @Bean
    public AutoFillInterceptor autoFillInterceptor() {
        return new AutoFillInterceptor();
    }
}
