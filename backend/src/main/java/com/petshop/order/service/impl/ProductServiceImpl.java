package com.petshop.order.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.petshop.order.common.BusinessException;
import com.petshop.order.common.PageResult;
import com.petshop.order.entity.Product;
import com.petshop.order.entity.Sku;
import com.petshop.order.mapper.ProductMapper;
import com.petshop.order.mapper.SkuMapper;
import com.petshop.order.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;

    @Override
    public PageResult<Product> getList(int page, int size, String keyword, Long categoryId, String type, String status) {
        PageHelper.startPage(page, size);
        List<Product> list = productMapper.selectPageList(keyword, categoryId, type, status);
        PageInfo<Product> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal(), page, size);
    }

    @Override
    public Product getDetail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (product.getSkus() == null || product.getSkus().isEmpty()) {
            product.setSkus(skuMapper.selectByProductId(id));
        }
        return product;
    }

    @Override
    @Transactional
    public Product create(Product product) {
        if ("SERVICE".equals(product.getType())) {
            product.setSupportDelivery(0);
        }
        if (product.getStatus() == null) {
            product.setStatus("ON_SALE");
        }
        productMapper.insert(product);

        List<Sku> skus = product.getSkus();
        if (skus != null && !skus.isEmpty()) {
            for (Sku sku : skus) {
                sku.setProductId(product.getId());
            }
            skuMapper.insertBatch(skus);
        }

        return getDetail(product.getId());
    }

    @Override
    @Transactional
    public Product update(Long id, Product product) {
        Product existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("商品不存在");
        }
        product.setId(id);
        if ("SERVICE".equals(product.getType())) {
            product.setSupportDelivery(0);
        }
        productMapper.updateById(product);

        List<Sku> skus = product.getSkus();
        if (skus != null) {
            skuMapper.deleteByProductId(id);
            if (!skus.isEmpty()) {
                for (Sku sku : skus) {
                    sku.setProductId(id);
                }
                skuMapper.insertBatch(skus);
            }
        }

        return getDetail(id);
    }

    @Override
    public void updateStatus(Long id, String status) {
        Product existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("商品不存在");
        }
        productMapper.updateStatus(id, status);
    }

    @Override
    public void delete(Long id) {
        Product existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("商品不存在");
        }
        int count = productMapper.existsOrderItemByProductId(id);
        if (count > 0) {
            throw new BusinessException("该商品已被订单引用，无法删除");
        }
        skuMapper.deleteByProductId(id);
        productMapper.deleteById(id);
    }
}
