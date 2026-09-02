package com.petshop.order.controller.app;

import com.petshop.order.common.R;
import com.petshop.order.entity.Shop;
import com.petshop.order.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * C 端门店列表（免登）：换店页、进店链接校验用。
 */
@RestController
@RequestMapping("/api/app/shops")
@RequiredArgsConstructor
public class AppShopController {

    private final ShopService shopService;

    @GetMapping
    public R<List<Map<String, Object>>> list() {
        List<Map<String, Object>> list = shopService.listAll().stream().map(this::toMap).toList();
        return R.ok(list);
    }

    private Map<String, Object> toMap(Shop shop) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", shop.getId());
        m.put("code", shop.getCode());
        m.put("name", shop.getName());
        m.put("phone", shop.getPhone());
        m.put("address", shop.getAddress());
        m.put("status", shop.getStatus());
        return m;
    }
}
