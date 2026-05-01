package com.petshop.order.service;

import com.petshop.order.entity.Category;

import java.util.List;

public interface CategoryService {

    List<Category> getList(String type);

    Category create(Category category);

    Category update(Long id, Category category);

    void delete(Long id);
}
