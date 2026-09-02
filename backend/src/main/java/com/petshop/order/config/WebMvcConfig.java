package com.petshop.order.config;

import com.petshop.order.interceptor.ShopResolveInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private final ShopResolveInterceptor shopResolveInterceptor;

    public WebMvcConfig(ShopResolveInterceptor shopResolveInterceptor) {
        this.shopResolveInterceptor = shopResolveInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + new java.io.File(uploadDir).getAbsolutePath() + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 门店上下文解析：在 SaInterceptor（order=0）之后执行，管理端可读取登录态
        registry.addInterceptor(shopResolveInterceptor)
                .addPathPatterns("/api/app/**", "/api/admin/**")
                .order(1);
    }
}
