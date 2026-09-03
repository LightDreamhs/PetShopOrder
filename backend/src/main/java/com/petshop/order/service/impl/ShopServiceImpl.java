package com.petshop.order.service.impl;

import com.petshop.order.common.BusinessException;
import com.petshop.order.common.ShopContext;
import com.petshop.order.entity.Shop;
import com.petshop.order.entity.ShopConfig;
import com.petshop.order.mapper.ShopConfigMapper;
import com.petshop.order.mapper.ShopMapper;
import com.petshop.order.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z0-9-]{2,32}$");

    private final ShopMapper shopMapper;
    private final ShopConfigMapper shopConfigMapper;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Shop create(Shop shop) {
        validate(shop, null);
        if (shop.getStatus() == null || shop.getStatus().isEmpty()) {
            shop.setStatus("OPEN");
        }
        if (shop.getSort() == null) {
            shop.setSort(0);
        }
        shopMapper.insert(shop);
        // 初始化默认店铺配置（配送半径 5km / 免运费 / 不限时段，BOSS 后台可改）
        ShopConfig config = new ShopConfig();
        config.setShopId(shop.getId());
        config.setShopLat(shop.getShopLat());
        config.setShopLng(shop.getShopLng());
        config.setDeliveryRadiusKm(new java.math.BigDecimal("5.00"));
        config.setDeliveryMinAmount(new java.math.BigDecimal("20.00"));
        config.setDeliveryFeeType("FREE");
        config.setOrderTimeEnabled(0);
        config.setHasQywxWebhook(0);
        config.setAdEnabled(0);
        shopConfigMapper.insert(config);
        return shop;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Shop update(Long id, Shop shop) {
        Shop existing = shopMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("门店不存在");
        }
        validate(shop, id);
        shop.setId(id);
        shopMapper.updateById(shop);
        // 坐标同步到店铺配置（配送距离以 shop_config 为准）
        ShopConfig config = shopConfigMapper.selectByShopId(id);
        if (config != null) {
            config.setShopLat(shop.getShopLat());
            config.setShopLng(shop.getShopLng());
            shopConfigMapper.updateById(config);
        }
        return shopMapper.selectById(id);
    }

    @Override
    public void updateStatus(Long id, String status) {
        Shop existing = shopMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("门店不存在");
        }
        shopMapper.updateStatus(id, status);
    }

    private void validate(Shop shop, Long excludeId) {
        if (shop.getCode() == null || !CODE_PATTERN.matcher(shop.getCode()).matches()) {
            throw new BusinessException("店铺编码须为 2~32 位小写字母/数字/连字符（如 erjiangsi）");
        }
        if (shop.getName() == null || shop.getName().isEmpty()) {
            throw new BusinessException("店铺名称不能为空");
        }
        if (shop.getShopLat() == null || shop.getShopLng() == null) {
            throw new BusinessException("店铺坐标（纬度/经度）不能为空");
        }
        Shop byCode = shopMapper.selectByCode(shop.getCode());
        if (byCode != null && !byCode.getId().equals(excludeId)) {
            throw new BusinessException("店铺编码已被使用: " + shop.getCode());
        }
    }
}
