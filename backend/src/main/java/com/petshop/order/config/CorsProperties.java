package com.petshop.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨域配置，绑定 app.cors.* 命名空间。
 *
 * allowed-origins 支持通配 pattern（如 http://192.168.*.*:3000），
 * 生产同域访问可留空（不触发 CORS）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();
}
