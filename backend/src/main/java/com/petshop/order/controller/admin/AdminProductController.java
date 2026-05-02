package com.petshop.order.controller.admin;

import com.petshop.order.common.PageResult;
import com.petshop.order.common.R;
import com.petshop.order.entity.Product;
import com.petshop.order.entity.Sku;
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

    @GetMapping
    public R<Map<String, Object>> getList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        PageResult<Product> pr = productService.getList(page, size, keyword, categoryId, type, status);
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
        Product product = productService.getDetail(id);
        return R.ok(toDetailMap(product));
    }

    @PostMapping
    public R<Map<String, Object>> create(@Validated @RequestBody ProductRequest req) {
        Product product = toEntity(req);
        Product saved = productService.create(product);
        return R.ok(toDetailMap(saved));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable Long id, @Validated @RequestBody ProductRequest req) {
        Product product = toEntity(req);
        Product updated = productService.update(id, product);
        return R.ok(toDetailMap(updated));
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @Validated @RequestBody StatusRequest req) {
        productService.updateStatus(id, req.getStatus());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return R.ok();
    }

    private Product toEntity(ProductRequest req) {
        Product p = new Product();
        p.setCategoryId(req.getCategoryId());
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setCoverImg(req.getCoverImg());
        p.setType(req.getType());
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
        sku.setStock(s.getStock() != null ? s.getStock() : 0);
        sku.setSort(s.getSort() != null ? s.getSort() : 0);
        return sku;
    }

    private Map<String, Object> toListMap(Product p) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("coverImg", p.getCoverImg() != null ? p.getCoverImg() : "");
        m.put("categoryName", p.getCategoryName() != null ? p.getCategoryName() : "");
        m.put("type", p.getType());
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
        m.put("categoryId", p.getCategoryId());
        m.put("categoryName", p.getCategoryName() != null ? p.getCategoryName() : "");
        m.put("name", p.getName());
        m.put("description", p.getDescription() != null ? p.getDescription() : "");
        m.put("coverImg", p.getCoverImg() != null ? p.getCoverImg() : "");
        m.put("type", p.getType());
        m.put("status", p.getStatus());
        m.put("supportDelivery", p.getSupportDelivery() != null && p.getSupportDelivery() == 1);
        m.put("sort", p.getSort());
        m.put("createTime", p.getCreateTime() != null ? p.getCreateTime().format(FMT) : "");
        m.put("skus", p.getSkus() != null ? p.getSkus().stream().map(this::toSkuMap).toList() : List.of());
        return m;
    }

    private Map<String, Object> toSkuMap(Sku s) {
        return Map.of(
                "id", s.getId(),
                "specName", s.getSpecName(),
                "price", s.getPrice().toPlainString(),
                "memberPrice", s.getMemberPrice() != null ? s.getMemberPrice().toPlainString() : "",
                "stock", s.getStock(),
                "sort", s.getSort()
        );
    }

    @Data
    public static class ProductRequest {
        @NotNull
        private Long categoryId;
        @NotBlank
        private String name;
        private String description;
        private String coverImg;
        @NotBlank
        private String type;
        private Boolean supportDelivery;
        private Integer sort;
        @Valid
        private List<SkuRequest> skus;
    }

    @Data
    public static class SkuRequest {
        @NotBlank
        private String specName;
        @NotNull
        private BigDecimal price;
        private BigDecimal memberPrice;
        private Integer stock;
        private Integer sort;
    }

    @Data
    public static class StatusRequest {
        @NotBlank
        @jakarta.validation.constraints.Pattern(regexp = "ON_SALE|OFF_SALE", message = "商品状态不合法")
        private String status;
    }
}
