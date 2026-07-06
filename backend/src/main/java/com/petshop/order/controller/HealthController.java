package com.petshop.order.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查端点（无鉴权）。
 *
 * 供 nginx 健康探针 / 容器编排 liveness probe 调用。
 * 路径 /health 不在 /api/** 下，Sa-Token 拦截器（SaTokenConfig 仅拦截 /api/**）不介入。
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "petshop-order-backend"
        );
    }
}
