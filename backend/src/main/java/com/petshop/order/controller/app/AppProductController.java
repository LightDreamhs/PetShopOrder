package com.petshop.order.controller.app;

import com.petshop.order.common.R;
import com.petshop.order.entity.Category;
import com.petshop.order.entity.Product;
import com.petshop.order.entity.Sku;
import com.petshop.order.mapper.CategoryMapper;
import com.petshop.order.mapper.ProductMapper;
import com.petshop.order.mapper.SkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
public class AppProductController {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;

    @GetMapping("/categories")
    public R<List<Map<String, Object>>> getCategoryList(@RequestParam(required = false) String type) {
        List<Category> list = categoryMapper.selectList(type);
        List<Map<String, Object>> result = list.stream().map(c -> Map.<String, Object>of(
                "id", c.getId(),
                "name", c.getName(),
                "icon", c.getIcon() != null ? c.getIcon() : "",
                "type", c.getType(),
                "sort", c.getSort()
        )).toList();
        return R.ok(result);
    }

    @GetMapping("/products")
    public R<List<Map<String, Object>>> getProductList(@RequestParam(required = false) String type) {
        List<Product> list = productMapper.selectPageList(null, null, type, "ON_SALE");
        List<Map<String, Object>> result = list.stream().map(this::toAppMap).toList();
        return R.ok(result);
    }

    @GetMapping("/categories/{categoryId}/products")
    public R<List<Map<String, Object>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) String keyword) {
        List<Product> list = productMapper.selectPageList(keyword, categoryId, null, "ON_SALE");
        List<Map<String, Object>> result = list.stream().map(this::toAppMap).toList();
        return R.ok(result);
    }

    @GetMapping("/products/{id}")
    public R<Map<String, Object>> getDetail(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || !"ON_SALE".equals(product.getStatus())) {
            return R.fail("商品不存在");
        }
        List<Sku> skus = skuMapper.selectByProductId(id);

        String price = "0.00";
        if (product.getMinPrice() != null) {
            price = product.getMinPrice().toPlainString();
        } else if (!skus.isEmpty()) {
            price = skus.stream()
                    .map(Sku::getPrice)
                    .min(BigDecimal::compareTo)
                    .map(BigDecimal::toPlainString)
                    .orElse("0.00");
        }

        Map<String, Object> result = Map.of(
                "id", product.getId(),
                "name", product.getName(),
                "description", product.getDescription() != null ? product.getDescription() : "",
                "coverImg", product.getCoverImg() != null ? product.getCoverImg() : "",
                "type", product.getType(),
                "supportDelivery", product.getSupportDelivery() != null && product.getSupportDelivery() == 1,
                "price", price,
                "dealPrice", price,
                "skus", skus.stream().map(s -> Map.<String, Object>of(
                        "id", s.getId(),
                        "specName", s.getSpecName(),
                        "price", s.getPrice().toPlainString(),
                        "dealPrice", s.getPrice().toPlainString(),
                        "stock", s.getStock()
                )).toList()
        );
        return R.ok(result);
    }

    private Map<String, Object> toAppMap(Product p) {
        String price = p.getMinPrice() != null ? p.getMinPrice().toPlainString() : "0.00";
        int skuCount = p.getSkuCount() != null ? p.getSkuCount() : 0;
        return Map.of(
                "id", p.getId(),
                "name", p.getName(),
                "coverImg", p.getCoverImg() != null ? p.getCoverImg() : "",
                "type", p.getType(),
                "supportDelivery", p.getSupportDelivery() != null && p.getSupportDelivery() == 1,
                "price", price,
                "dealPrice", price,
                "hasSpec", skuCount > 1
        );
    }
}
