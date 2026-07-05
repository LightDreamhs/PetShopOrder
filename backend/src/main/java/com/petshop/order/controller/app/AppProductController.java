package com.petshop.order.controller.app;

import cn.dev33.satoken.stp.StpUtil;
import com.petshop.order.common.R;
import com.petshop.order.entity.*;
import com.petshop.order.mapper.MemberLevelMapper;
import com.petshop.order.mapper.MemberMapper;
import com.petshop.order.mapper.MemberPhoneMapper;
import com.petshop.order.mapper.ProductMapper;
import com.petshop.order.mapper.SkuMapper;
import com.petshop.order.service.AppAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
public class AppProductController {

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final AppAuthService appAuthService;
    private final MemberPhoneMapper memberPhoneMapper;
    private final MemberMapper memberMapper;
    private final MemberLevelMapper memberLevelMapper;

    @GetMapping("/products")
    public R<List<Map<String, Object>>> getProductList(@RequestParam(required = false) String type) {
        List<Product> list = productMapper.selectPageList(null, type, "ON_SALE");
        BigDecimal discountRate = getMemberDiscountRate();
        List<Map<String, Object>> result = list.stream().map(p -> toAppMap(p, discountRate)).toList();
        return R.ok(result);
    }

    @GetMapping("/products/{id}")
    public R<Map<String, Object>> getDetail(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || !"ON_SALE".equals(product.getStatus())) {
            return R.fail("商品不存在");
        }
        List<Sku> skus = skuMapper.selectByProductId(id);
        BigDecimal discountRate = getMemberDiscountRate();

        String price = "0.00";
        BigDecimal minPriceBd = null;
        if (product.getMinPrice() != null) {
            minPriceBd = product.getMinPrice();
        } else if (!skus.isEmpty()) {
            minPriceBd = skus.stream()
                    .map(Sku::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        }
        if (minPriceBd != null) {
            price = minPriceBd.toPlainString();
        }

        String dealPrice = price;
        if (!skus.isEmpty()) {
            Sku cheapest = skus.stream()
                    .min(Comparator.comparing(Sku::getPrice))
                    .orElse(skus.get(0));
            dealPrice = calcDealPrice(cheapest.getPrice(), cheapest.getMemberPrice(), discountRate, product.getType());
        }

        Map<String, Object> result = Map.of(
                "id", product.getId(),
                "name", product.getName(),
                "description", product.getDescription() != null ? product.getDescription() : "",
                "coverImg", product.getCoverImg() != null ? product.getCoverImg() : "",
                "type", product.getType(),
                "serviceCategory", product.getServiceCategory() != null ? product.getServiceCategory() : "",
                "supportDelivery", product.getSupportDelivery() != null && product.getSupportDelivery() == 1,
                "price", price,
                "dealPrice", dealPrice,
                "skus", skus.stream().map(s -> Map.<String, Object>of(
                        "id", s.getId(),
                        "specName", s.getSpecName(),
                        "price", s.getPrice().toPlainString(),
                        "duration", s.getDuration() != null ? s.getDuration() : 0,
                        "dealPrice", calcDealPrice(s.getPrice(), s.getMemberPrice(), discountRate, product.getType())
                )).toList()
        );
        return R.ok(result);
    }

    private BigDecimal getMemberDiscountRate() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        try {
            AppUser user = appAuthService.getCurrentUser();
            if (user == null || user.getPhone() == null || user.getPhone().isEmpty()) {
                return null;
            }
            Long memberId = memberPhoneMapper.selectMemberIdByPhone(user.getPhone());
            if (memberId == null) {
                return null;
            }
            Member member = memberMapper.selectById(memberId);
            if (member == null || member.getLevelId() == null) {
                return null;
            }
            MemberLevel level = memberLevelMapper.selectById(member.getLevelId());
            if (level == null || level.getDiscountRate() == null) {
                return null;
            }
            return level.getDiscountRate();
        } catch (Exception e) {
            return null;
        }
    }

    private String calcDealPrice(BigDecimal price, BigDecimal memberPrice, BigDecimal discountRate, String type) {
        if (discountRate == null) {
            return price.toPlainString();
        }
        BigDecimal result;
        if ("GOODS".equals(type)) {
            result = memberPrice != null ? memberPrice : price;
        } else if ("SERVICE".equals(type)) {
            result = price.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        } else {
            result = price;
        }
        return result.toPlainString();
    }

    private Map<String, Object> toAppMap(Product p, BigDecimal discountRate) {
        String price = p.getMinPrice() != null ? p.getMinPrice().toPlainString() : "0.00";
        int skuCount = p.getSkuCount() != null ? p.getSkuCount() : 0;

        String dealPrice = price;
        if (discountRate != null && p.getMinPrice() != null) {
            List<Sku> skus = skuMapper.selectByProductId(p.getId());
            if (!skus.isEmpty()) {
                Sku cheapest = skus.stream()
                        .min(Comparator.comparing(Sku::getPrice))
                        .orElse(skus.get(0));
                dealPrice = calcDealPrice(cheapest.getPrice(), cheapest.getMemberPrice(), discountRate, p.getType());
            }
        }

        return Map.of(
                "id", p.getId(),
                "name", p.getName(),
                "description", p.getDescription() != null ? p.getDescription() : "",
                "coverImg", p.getCoverImg() != null ? p.getCoverImg() : "",
                "type", p.getType(),
                "serviceCategory", p.getServiceCategory() != null ? p.getServiceCategory() : "",
                "supportDelivery", p.getSupportDelivery() != null && p.getSupportDelivery() == 1,
                "price", price,
                "dealPrice", dealPrice,
                "hasSpec", skuCount > 1
        );
    }
}
