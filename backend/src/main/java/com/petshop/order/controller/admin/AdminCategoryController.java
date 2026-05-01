package com.petshop.order.controller.admin;

import com.petshop.order.common.R;
import com.petshop.order.entity.Category;
import com.petshop.order.service.CategoryService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public R<List<Map<String, Object>>> getList(@RequestParam(required = false) String type) {
        List<Category> list = categoryService.getList(type);
        List<Map<String, Object>> result = list.stream().map(this::toMap).toList();
        return R.ok(result);
    }

    @PostMapping
    public R<Map<String, Object>> create(@Validated @RequestBody CategoryRequest req) {
        Category category = new Category();
        category.setName(req.getName());
        category.setIcon(req.getIcon());
        category.setType(req.getType());
        category.setSort(req.getSort() != null ? req.getSort() : 0);
        Category saved = categoryService.create(category);
        return R.ok(toMap(saved));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable Long id, @Validated @RequestBody CategoryUpdateRequest req) {
        Category category = new Category();
        category.setName(req.getName());
        category.setIcon(req.getIcon());
        category.setSort(req.getSort() != null ? req.getSort() : 0);
        Category updated = categoryService.update(id, category);
        return R.ok(toMap(updated));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }

    private Map<String, Object> toMap(Category c) {
        return Map.of(
                "id", c.getId(),
                "name", c.getName(),
                "icon", c.getIcon() != null ? c.getIcon() : "",
                "type", c.getType(),
                "sort", c.getSort(),
                "productCount", c.getProductCount() != null ? c.getProductCount() : 0
        );
    }

    @Data
    public static class CategoryRequest {
        @NotBlank
        private String name;
        private String icon;
        @NotBlank
        private String type;
        private Integer sort;
    }

    @Data
    public static class CategoryUpdateRequest {
        @NotBlank
        private String name;
        private String icon;
        private Integer sort;
    }
}
