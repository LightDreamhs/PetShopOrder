package com.petshop.order.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.petshop.order.common.BusinessException;
import com.petshop.order.common.PageResult;
import com.petshop.order.common.ShopContext;
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
    public PageResult<Product> getList(int page, int size, String keyword, String type, String status) {
        PageHelper.startPage(page, size);
        // 商品目录平台级；SKU 聚合值（数量/最低价）按当前店统计
        List<Product> list = productMapper.selectPageList(keyword, type, status, ShopContext.require());
        PageInfo<Product> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal(), page, size);
    }

    @Override
    public Product getDetail(Long id) {
        Product product = productMapper.selectById(id, ShopContext.require(), false);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (product.getSkus() == null || product.getSkus().isEmpty()) {
            product.setSkus(skuMapper.selectByProductId(id, ShopContext.require()));
        }
        return product;
    }

    @Override
    @Transactional
    public Product create(Product product) {
        normalizeServiceCategory(product);
        if ("SERVICE".equals(product.getType())) {
            product.setSupportDelivery(0);
        }
        if (product.getStatus() == null) {
            product.setStatus("ON_SALE");
        }
        productMapper.insert(product);

        List<Sku> skus = product.getSkus();
        if (skus != null && !skus.isEmpty()) {
            Long shopId = ShopContext.require();
            for (Sku sku : skus) {
                sku.setProductId(product.getId());
                sku.setShopId(shopId);
            }
            skuMapper.insertBatch(skus);
        }

        return getDetail(product.getId());
    }

    @Override
    @Transactional
    public Product update(Long id, Product product) {
        Product existing = productMapper.selectById(id, ShopContext.require(), false);
        if (existing == null) {
            throw new BusinessException("商品不存在");
        }
        product.setId(id);
        normalizeServiceCategory(product);
        if ("SERVICE".equals(product.getType())) {
            product.setSupportDelivery(0);
        }
        productMapper.updateById(product);

        List<Sku> skus = product.getSkus();
        if (skus != null) {
            // SKU 店铺级：只重建当前店的 SKU，其他店价格不受影响
            Long shopId = ShopContext.require();
            skuMapper.deleteByProductIdAndShop(id, shopId);
            if (!skus.isEmpty()) {
                for (Sku sku : skus) {
                    sku.setProductId(id);
                    sku.setShopId(shopId);
                }
                skuMapper.insertBatch(skus);
            }
        }

        return getDetail(id);
    }

    /**
     * 规整服务子类：GOODS 强制 null；SERVICE 缺省 MAIN_SERVICE；其他类型清空。
     */
    private void normalizeServiceCategory(Product product) {
        if ("SERVICE".equals(product.getType())) {
            String cat = product.getServiceCategory();
            if (cat == null || cat.isEmpty()) {
                product.setServiceCategory("MAIN_SERVICE");
            }
        } else {
            product.setServiceCategory(null);
        }
    }

    @Override
    public void updateStatus(Long id, String status) {
        Product existing = productMapper.selectById(id, ShopContext.require(), false);
        if (existing == null) {
            throw new BusinessException("商品不存在");
        }
        productMapper.updateStatus(id, status);
    }

    @Override
    public void delete(Long id) {
        Product existing = productMapper.selectById(id, ShopContext.require(), false);
        if (existing == null) {
            throw new BusinessException("商品不存在");
        }
        int count = productMapper.existsOrderItemByProductId(id);
        if (count > 0) {
            throw new BusinessException("该商品已被订单引用，无法删除");
        }
        // 目录删除：清掉该商品在所有店的 SKU
        skuMapper.deleteByProductId(id);
        productMapper.deleteById(id);
    }
}
