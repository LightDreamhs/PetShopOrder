package com.petshop.order.controller.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.petshop.order.common.BusinessException;
import com.petshop.order.common.PageResult;
import com.petshop.order.common.R;
import com.petshop.order.entity.Product;
import com.petshop.order.entity.Sku;
import com.petshop.order.mapper.MainServiceAddonMapper;
import com.petshop.order.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProductService productService;
    private final MainServiceAddonMapper mainServiceAddonMapper;

    private void checkManager() {
        if (!StpUtil.hasRole("BOSS") && !StpUtil.hasRole("MANAGER")) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    @GetMapping
    public R<Map<String, Object>> getList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        checkManager();
        PageResult<Product> pr = productService.getList(page, size, keyword, type, status);
        List<Map<String, Object>> list = pr.getList().stream().map(this::toListMap).toList();
        Map<String, Object> result = Map.of(
                "list", list,
                "total", pr.getTotal(),
                "page", pr.getPage(),
                "size", pr.getSize()
        );
        return R.ok(result);
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> getDetail(@PathVariable Long id) {
        checkManager();
        Product product = productService.getDetail(id);
        return R.ok(toDetailMap(product));
    }

    @PostMapping
    public R<Map<String, Object>> create(@Validated @RequestBody ProductRequest req) {
        checkManager();
        Product product = toEntity(req);
        Product saved = productService.create(product);
        // 主服务：覆盖式更新附加服务绑定
        replaceAddons(saved.getId(), req);
        return R.ok(toDetailMap(saved));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable Long id, @Validated @RequestBody ProductRequest req) {
        checkManager();
        Product product = toEntity(req);
        Product updated = productService.update(id, product);
        // 主服务：覆盖式更新附加服务绑定（非主服务清空其作为 main 的绑定）
        replaceAddons(updated.getId(), req);
        return R.ok(toDetailMap(updated));
    }

    /**
     * 覆盖式更新某主服务的附加服务绑定（先删后插）。
     * 仅当 type=SERVICE 且 serviceCategory=MAIN_SERVICE 时生效。
     */
    private void replaceAddons(Long productId, ProductRequest req) {
        if (!"SERVICE".equals(req.getType()) || !"MAIN_SERVICE".equals(req.getServiceCategory())) {
            return;
        }
        List<Long> addonIds = req.getAddonProductIds() != null ? req.getAddonProductIds() : List.of();
        mainServiceAddonMapper.deleteByMainProductId(productId);
        if (!addonIds.isEmpty()) {
            mainServiceAddonMapper.insertBatch(productId, addonIds);
        }
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @Validated @RequestBody StatusRequest req) {
        checkManager();
        productService.updateStatus(id, req.getStatus());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        checkManager();
        productService.delete(id);
        return R.ok();
    }

    private Product toEntity(ProductRequest req) {
        Product p = new Product();
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setCoverImg(req.getCoverImg());
        p.setType(req.getType());
        // 服务子类：GOODS 强制 null；SERVICE 缺省 MAIN_SERVICE
        if ("GOODS".equals(req.getType())) {
            p.setServiceCategory(null);
        } else if ("SERVICE".equals(req.getType())) {
            String cat = req.getServiceCategory();
            p.setServiceCategory(cat != null && !cat.isEmpty() ? cat : "MAIN_SERVICE");
        }
        p.setSupportDelivery(req.getSupportDelivery() != null && req.getSupportDelivery() ? 1 : 0);
        p.setSort(req.getSort() != null ? req.getSort() : 0);
        if (req.getSkus() != null) {
            p.setSkus(req.getSkus().stream().map(this::toSku).toList());
        }
        return p;
    }

    private Sku toSku(SkuRequest s) {
        Sku sku = new Sku();
        sku.setSpecName(s.getSpecName());
        sku.setPrice(s.getPrice());
        sku.setMemberPrice(s.getMemberPrice());
        sku.setDuration(s.getDuration());
        sku.setStock(-1);
        sku.setSort(s.getSort() != null ? s.getSort() : 0);
        return sku;
    }

    private Map<String, Object> toListMap(Product p) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("coverImg", p.getCoverImg() != null ? p.getCoverImg() : "");
        m.put("type", p.getType());
        m.put("serviceCategory", p.getServiceCategory() != null ? p.getServiceCategory() : "");
        m.put("status", p.getStatus());
        m.put("supportDelivery", p.getSupportDelivery() != null && p.getSupportDelivery() == 1);
        m.put("sort", p.getSort());
        m.put("skuCount", p.getSkuCount() != null ? p.getSkuCount() : 0);
        m.put("minPrice", p.getMinPrice() != null ? p.getMinPrice().toPlainString() : "0.00");
        m.put("createTime", p.getCreateTime() != null ? p.getCreateTime().format(FMT) : "");
        return m;
    }

    private Map<String, Object> toDetailMap(Product p) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("description", p.getDescription() != null ? p.getDescription() : "");
        m.put("coverImg", p.getCoverImg() != null ? p.getCoverImg() : "");
        m.put("type", p.getType());
        m.put("serviceCategory", p.getServiceCategory() != null ? p.getServiceCategory() : "");
        m.put("status", p.getStatus());
        m.put("supportDelivery", p.getSupportDelivery() != null && p.getSupportDelivery() == 1);
        m.put("sort", p.getSort());
        m.put("createTime", p.getCreateTime() != null ? p.getCreateTime().format(FMT) : "");
        m.put("skus", p.getSkus() != null ? p.getSkus().stream().map(this::toSkuMap).toList() : List.of());
        // 主服务：回显已绑定的附加服务 id 列表
        m.put("addonProductIds", mainServiceAddonMapper.selectAddonIdsByMainProductId(p.getId()));
        return m;
    }

    private Map<String, Object> toSkuMap(Sku s) {
        return Map.of(
                "id", s.getId(),
                "specName", s.getSpecName(),
                "price", s.getPrice().toPlainString(),
                "memberPrice", s.getMemberPrice() != null ? s.getMemberPrice().toPlainString() : "",
                "duration", s.getDuration() != null ? s.getDuration() : 0,
                "sort", s.getSort()
        );
    }

    @Data
    public static class ProductRequest {
        @NotBlank
        private String name;
        private String description;
        private String coverImg;
        @NotBlank
        private String type;
        private String serviceCategory;
        private Boolean supportDelivery;
        private Integer sort;
        @Valid
        private List<SkuRequest> skus;
        /** 主服务绑定的附加服务 product id 列表（仅 type=SERVICE&serviceCategory=MAIN_SERVICE 时生效） */
        private List<Long> addonProductIds;
    }

    @Data
    public static class SkuRequest {
        @NotBlank
        private String specName;
        @NotNull
        private BigDecimal price;
        private BigDecimal memberPrice;
        private Integer duration;
        private Integer sort;
    }

    @Data
    public static class StatusRequest {
        @NotBlank
        @jakarta.validation.constraints.Pattern(regexp = "ON_SALE|OFF_SALE", message = "商品状态不合法")
        private String status;
    }
}
