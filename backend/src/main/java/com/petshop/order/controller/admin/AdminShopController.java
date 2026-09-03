package com.petshop.order.controller.admin;

import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.petshop.order.config.StpAdminUtil;
import com.petshop.order.common.BusinessException;
import com.petshop.order.common.R;
import com.petshop.order.common.ShopContext;
import com.petshop.order.entity.Shop;
import com.petshop.order.service.ShopService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 门店管理（BOSS）：店铺 CRUD、启停、进店二维码生成。
 * /current 为所有管理端员工可用（展示当前操作门店）。
 */
@RestController
@RequestMapping("/api/admin/shops")
@RequiredArgsConstructor
public class AdminShopController {

    private final ShopService shopService;

    @Value("${app.shop-url-base:https://2zg.site/}")
    private String shopUrlBase;

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

    @PostMapping
    public R<Map<String, Object>> create(@Validated @RequestBody ShopRequest req) {
        StpAdminUtil.checkRole("BOSS");
        Shop created = shopService.create(toEntity(req));
        return R.ok(toMap(created));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable Long id, @Validated @RequestBody ShopRequest req) {
        StpAdminUtil.checkRole("BOSS");
        Shop updated = shopService.update(id, toEntity(req));
        return R.ok(toMap(updated));
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @Validated @RequestBody StatusRequest req) {
        StpAdminUtil.checkRole("BOSS");
        shopService.updateStatus(id, req.getStatus());
        return R.ok();
    }

    /**
     * 进店二维码 PNG（内容 = {shop-url-base}?s={code}）。
     * 供后台展示/下载打印；本地联调可通过 SHOP_URL_BASE 环境变量指向本机地址。
     */
    @GetMapping("/{id}/qrcode")
    public ResponseEntity<byte[]> qrcode(@PathVariable Long id) {
        StpAdminUtil.checkRole("BOSS");
        Shop shop = shopService.getById(id);
        if (shop == null) {
            throw new BusinessException("门店不存在");
        }
        String url = entryUrl(shop.getCode());
        QrConfig config = new QrConfig(400, 400);
        config.setMargin(2);
        byte[] png = QrCodeUtil.generatePng(url, config);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("Content-Disposition", "attachment; filename=shop-" + shop.getCode() + "-qrcode.png")
                .body(png);
    }

    private String entryUrl(String code) {
        String base = shopUrlBase.endsWith("/") ? shopUrlBase : shopUrlBase + "/";
        return base + "?s=" + code;
    }

    private Shop toEntity(ShopRequest req) {
        Shop shop = new Shop();
        shop.setCode(req.getCode());
        shop.setName(req.getName());
        shop.setPhone(req.getPhone());
        shop.setAddress(req.getAddress());
        shop.setShopLat(req.getShopLat());
        shop.setShopLng(req.getShopLng());
        shop.setSort(req.getSort() != null ? req.getSort() : 0);
        return shop;
    }

    private Map<String, Object> toMap(Shop shop) {
        Map<String, Object> m = new LinkedHashMap<>();
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
        m.put("entryUrl", entryUrl(shop.getCode()));
        return m;
    }

    @Data
    public static class ShopRequest {
        @NotBlank
        private String code;
        @NotBlank
        private String name;
        private String phone;
        private String address;
        @NotNull
        private BigDecimal shopLat;
        @NotNull
        private BigDecimal shopLng;
        private Integer sort;
    }

    @Data
    public static class StatusRequest {
        @NotBlank
        @jakarta.validation.constraints.Pattern(regexp = "OPEN|CLOSED", message = "状态不合法")
        private String status;
    }
}
