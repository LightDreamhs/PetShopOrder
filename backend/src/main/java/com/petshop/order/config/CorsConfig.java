package com.petshop.order.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final CorsProperties corsProperties;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = corsProperties.getAllowedOrigins();
        if (origins == null || origins.isEmpty()) {
            // 现代浏览器对同源 fetch 也会带 Origin 头，CorsFilter 见 Origin 即校验；
            // 空白名单会拒绝所有带 Origin 的请求（含同源）。故默认放行所有来源，
            // 生产如需收敛，经 app.cors.allowed-origins 配置具体域名（非空则按配置）。
            config.addAllowedOriginPattern("*");
        } else {
            for (String origin : origins) {
                config.addAllowedOriginPattern(origin);
            }
        }
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
