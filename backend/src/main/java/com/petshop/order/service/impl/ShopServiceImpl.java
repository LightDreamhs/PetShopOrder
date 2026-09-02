package com.petshop.order.service.impl;

import com.petshop.order.common.BusinessException;
import com.petshop.order.common.ShopContext;
import com.petshop.order.entity.Shop;
import com.petshop.order.mapper.ShopMapper;
import com.petshop.order.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;

    @Override
    public Shop getById(Long id) {
        return id == null ? null : shopMapper.selectById(id);
    }

    @Override
    public Shop resolveByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        return shopMapper.selectByCode(code.trim());
    }

    @Override
    public Shop getDefaultShop() {
        return shopMapper.selectDefault();
    }

    @Override
    public List<Shop> listAll() {
        return shopMapper.selectAll();
    }

    @Override
    public Shop requireCurrentShop() {
        Long shopId = ShopContext.require();
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException("当前门店不存在");
        }
        return shop;
    }

    @Override
    public Shop requireOpenShop(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException("门店不存在");
        }
        if (!"OPEN".equals(shop.getStatus())) {
            throw new BusinessException("门店歇业中，暂不接受下单与预约");
        }
        return shop;
    }
}
