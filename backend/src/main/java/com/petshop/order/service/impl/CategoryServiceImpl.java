package com.petshop.order.service.impl;

import com.petshop.order.common.BusinessException;
import com.petshop.order.entity.Category;
import com.petshop.order.mapper.CategoryMapper;
import com.petshop.order.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<Category> getList(String type) {
        return categoryMapper.selectList(type);
    }

    @Override
    public Category create(Category category) {
        categoryMapper.insert(category);
        return categoryMapper.selectById(category.getId());
    }

    @Override
    public Category update(Long id, Category category) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("分类不存在");
        }
        category.setId(id);
        categoryMapper.updateById(category);
        return categoryMapper.selectById(id);
    }

    @Override
    public void delete(Long id) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("分类不存在");
        }
        int count = categoryMapper.countProductsById(id);
        if (count > 0) {
            throw new BusinessException("该分类下存在商品，无法删除");
        }
        categoryMapper.deleteById(id);
    }
}
