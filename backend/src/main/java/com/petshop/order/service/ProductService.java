package com.petshop.order.service;

import com.petshop.order.common.PageResult;
import com.petshop.order.entity.Product;

public interface ProductService {

    PageResult<Product> getList(int page, int size, String keyword, String type, String status);

    Product getDetail(Long id);

    Product create(Product product);

    Product update(Long id, Product product);

    void updateStatus(Long id, String status);

    void delete(Long id);
}
