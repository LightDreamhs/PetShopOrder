package com.petshop.order.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/app/auth/sms-code",
                        "/api/app/auth/login",
                        "/api/app/auth/check",
                        "/api/app/categories",
                        "/api/app/products",
                        "/api/app/categories/*/products",
                        "/api/admin/auth/login"
                );
    }
}
