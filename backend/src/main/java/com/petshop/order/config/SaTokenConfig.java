package com.petshop.order.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
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
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // 管理端全部接口强制 admin 体系登录（loginType=admin，独立 cookie satoken-admin），
                    // 角色细分由各控制器 checkRole/hasRole 完成
                    SaRouter.match("/api/admin/**")
                            .notMatch("/api/admin/auth/login")
                            .check(r -> StpAdminUtil.checkLogin());
                }))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/app/auth/sms-code",
                        "/api/app/auth/login",
                        "/api/app/auth/check",
                        "/api/app/products",
                        "/api/app/products/**",
                        "/api/app/shops",
                        "/api/app/system-config/public",
                        "/api/admin/auth/login"
                );
    }
}
