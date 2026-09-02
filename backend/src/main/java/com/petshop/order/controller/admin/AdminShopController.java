package com.petshop.order.controller.admin;

import com.petshop.order.config.StpAdminUtil;
import com.petshop.order.common.R;
import com.petshop.order.common.ShopContext;
import com.petshop.order.entity.Shop;
import com.petshop.order.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 门店管理（Phase 1 仅提供只读接口；建店/编辑/启停 UI 在 Phase 2）。
 */
@RestController
@RequestMapping("/api/admin/shops")
@RequiredArgsConstructor
public class AdminShopController {

    private final ShopService shopService;

    @GetMapping
    public R<List<Map<String, Object>>> list() {
        StpAdminUtil.checkRole("BOSS");
        List<Map<String, Object>> list = shopService.listAll().stream().map(this::toMap).toList();
        return R.ok(list);
    }

    /** 当前请求上下文中的门店（供前端展示"当前正在操作哪家店"） */
    @GetMapping("/current")
    public R<Map<String, Object>> current() {
        Shop shop = shopService.requireCurrentShop();
        return R.ok(toMap(shop));
    }

    private Map<String, Object> toMap(Shop shop) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", shop.getId());
        m.put("code", shop.getCode());
        m.put("name", shop.getName());
        m.put("phone", shop.getPhone());
        m.put("address", shop.getAddress());
        m.put("shopLat", shop.getShopLat());
        m.put("shopLng", shop.getShopLng());
        m.put("status", shop.getStatus());
        m.put("sort", shop.getSort());
        m.put("isCurrent", shop.getId().equals(ShopContext.get()));
        return m;
    }
}
