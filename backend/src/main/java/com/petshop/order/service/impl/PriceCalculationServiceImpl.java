package com.petshop.order.service.impl;

import com.petshop.order.common.BusinessException;
import com.petshop.order.entity.Member;
import com.petshop.order.entity.MemberLevel;
import com.petshop.order.entity.Product;
import com.petshop.order.entity.Sku;
import com.petshop.order.mapper.MemberLevelMapper;
import com.petshop.order.mapper.MemberMapper;
import com.petshop.order.mapper.MemberPhoneMapper;
import com.petshop.order.mapper.ProductMapper;
import com.petshop.order.mapper.SkuMapper;
import com.petshop.order.service.PriceCalculationService;
import com.petshop.order.service.dto.CalculatedItemResult;
import com.petshop.order.service.dto.CartItemInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriceCalculationServiceImpl implements PriceCalculationService {

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final MemberPhoneMapper memberPhoneMapper;
    private final MemberMapper memberMapper;
    private final MemberLevelMapper memberLevelMapper;

    @Override
    public Map<String, Object> calculateItems(List<CartItemInput> items, String userPhone) {
        BigDecimal discountRate = resolveDiscountRate(userPhone);
        boolean isMember = discountRate != null;

        List<CalculatedItemResult> calculatedItems = new ArrayList<>();
        BigDecimal goodsAmount = BigDecimal.ZERO;
        BigDecimal serviceAmount = BigDecimal.ZERO;

        for (CartItemInput input : items) {
            Product product = productMapper.selectById(input.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在: " + input.getProductId());
            }

            List<Sku> skus = skuMapper.selectByProductId(input.getProductId());
            Sku sku;
            if (input.getSkuId() != null) {
                sku = skus.stream()
                        .filter(s -> s.getId().equals(input.getSkuId()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("SKU不存在"));
            } else {
                sku = skus.stream()
                        .min(Comparator.comparingInt(s -> s.getSort() != null ? s.getSort() : 0))
                        .orElseThrow(() -> new BusinessException("商品无可用规格"));
            }

            BigDecimal originalPrice = sku.getPrice();
            BigDecimal dealPrice;

            if ("GOODS".equals(product.getType())) {
                if (isMember && sku.getMemberPrice() != null) {
                    dealPrice = sku.getMemberPrice();
                } else {
                    dealPrice = sku.getPrice();
                }
            } else if ("SERVICE".equals(product.getType())) {
                if (isMember) {
                    dealPrice = sku.getPrice().multiply(discountRate)
                            .setScale(2, RoundingMode.HALF_UP);
                } else {
                    dealPrice = sku.getPrice();
                }
            } else {
                dealPrice = sku.getPrice();
            }

            BigDecimal subtotal = dealPrice.multiply(new BigDecimal(input.getQuantity().toString()));

            CalculatedItemResult result = new CalculatedItemResult();
            result.setProductId(product.getId());
            result.setSkuId(sku.getId());
            result.setProductName(product.getName());
            result.setSkuName(sku.getSpecName());
            result.setType(product.getType());
            result.setOriginalPrice(originalPrice.toPlainString());
            result.setDealPrice(dealPrice.toPlainString());
            result.setQuantity(input.getQuantity());
            result.setSubtotal(subtotal.toPlainString());
            calculatedItems.add(result);

            if ("GOODS".equals(product.getType())) {
                goodsAmount = goodsAmount.add(subtotal);
            } else if ("SERVICE".equals(product.getType())) {
                serviceAmount = serviceAmount.add(subtotal);
            }
        }

        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("items", calculatedItems);
        resultMap.put("goodsAmount", goodsAmount.toPlainString());
        resultMap.put("serviceAmount", serviceAmount.toPlainString());
        return resultMap;
    }

    private BigDecimal resolveDiscountRate(String userPhone) {
        if (userPhone == null || userPhone.isEmpty()) {
            return null;
        }
        Long memberId = memberPhoneMapper.selectMemberIdByPhone(userPhone);
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
    }
}
