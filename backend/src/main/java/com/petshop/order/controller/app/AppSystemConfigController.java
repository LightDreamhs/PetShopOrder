package com.petshop.order.controller.app;

import com.petshop.order.common.R;
import com.petshop.order.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/app/system-config")
@RequiredArgsConstructor
public class AppSystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping("/public")
    public R<Map<String, Object>> getPublicConfig() {
        Map<String, Object> config = systemConfigService.getConfig();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) config.get("config");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentQrUrl", data.get("paymentQrUrl"));
        return R.ok(result);
    }
}
