package com.petshop.order.controller.app;

import com.petshop.order.common.R;
import com.petshop.order.entity.AppUser;
import com.petshop.order.entity.Product;
import com.petshop.order.entity.Sku;
import com.petshop.order.mapper.ProductMapper;
import com.petshop.order.mapper.SkuMapper;
import com.petshop.order.service.AppAuthService;
import com.petshop.order.service.DeliveryService;
import com.petshop.order.service.PriceCalculationService;
import com.petshop.order.service.dto.CalculatedItemResult;
import com.petshop.order.service.dto.CartItemInput;
import com.petshop.order.service.dto.DeliveryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/app/cart")
@RequiredArgsConstructor
public class AppCartController {

    private final PriceCalculationService priceCalculationService;
    private final DeliveryService deliveryService;
    private final AppAuthService appAuthService;
    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;

    @PostMapping("/calculate")
    public R<Map<String, Object>> calculate(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inputItems = (List<Map<String, Object>>) body.get("items");
        if (inputItems == null || inputItems.isEmpty()) {
            return R.ok(buildEmptyResult());
        }
        String deliveryLat = (String) body.get("deliveryLat");
        String deliveryLng = (String) body.get("deliveryLng");

        List<CartItemInput> cartItems = new ArrayList<>();
        List<DeliveryItem> deliveryItems = new ArrayList<>();

        for (Map<String, Object> item : inputItems) {
            Long productId = toLong(item.get("productId"));
            Long skuId = toLong(item.get("skuId"));
            Integer quantity = toInteger(item.get("quantity"));
            if (productId == null || quantity == null || quantity <= 0) {
                continue;
            }

            CartItemInput cartItem = new CartItemInput();
            cartItem.setProductId(productId);
            cartItem.setSkuId(skuId);
            cartItem.setQuantity(quantity);
            cartItems.add(cartItem);

            Product product = productMapper.selectById(productId);
            if (product != null) {
                List<Sku> skus = skuMapper.selectByProductId(productId);
                Sku sku;
                if (skuId != null) {
                    sku = skus.stream()
                            .filter(s -> s.getId().equals(skuId))
                            .findFirst().orElse(null);
                } else {
                    sku = skus.stream()
                            .min(Comparator.comparingInt(s -> s.getSort() != null ? s.getSort() : 0))
                            .orElse(null);
                }

                if (sku == null) {
                    continue;
                }

                DeliveryItem deliveryItem = new DeliveryItem();
                deliveryItem.setProductId(productId);
                deliveryItem.setSkuId(skuId);
                deliveryItem.setQuantity(quantity);
                deliveryItem.setType(product.getType());
                deliveryItem.setOriginalPrice(sku.getPrice());
                deliveryItem.setSupportDelivery(product.getSupportDelivery() != null && product.getSupportDelivery() == 1);
                deliveryItems.add(deliveryItem);
            }
        }

        String userPhone = null;
        try {
            AppUser currentUser = appAuthService.getCurrentUser();
            if (currentUser != null) {
                userPhone = currentUser.getPhone();
            }
        } catch (Exception ignored) {
        }

        Map<String, Object> priceResult = priceCalculationService.calculateItems(cartItems, userPhone);

        @SuppressWarnings("unchecked")
        List<CalculatedItemResult> calculatedItems = (List<CalculatedItemResult>) priceResult.get("items");
        String goodsAmount = (String) priceResult.get("goodsAmount");
        String serviceAmount = (String) priceResult.get("serviceAmount");

        Map<String, Object> deliveryCheck = null;
        String deliveryFee = "0.00";

        if (deliveryLat != null && !deliveryLat.isEmpty() && deliveryLng != null && !deliveryLng.isEmpty()) {
            deliveryCheck = deliveryService.checkDelivery(deliveryItems, deliveryLat, deliveryLng);
            Boolean canDeliver = (Boolean) deliveryCheck.get("canDeliver");
            if (Boolean.TRUE.equals(canDeliver)) {
                Object feeObj = deliveryCheck.get("deliveryFee");
                deliveryFee = feeObj != null ? feeObj.toString() : "0.00";
            }
        }

        BigDecimal total = new BigDecimal(goodsAmount)
                .add(new BigDecimal(serviceAmount))
                .add(new BigDecimal(deliveryFee));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", calculatedItems);
        result.put("goodsAmount", goodsAmount);
        result.put("serviceAmount", serviceAmount);
        result.put("deliveryFee", deliveryFee);
        result.put("totalAmount", total.toPlainString());
        result.put("deliveryCheck", deliveryCheck);

        return R.ok(result);
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private Integer toInteger(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }

    private Map<String, Object> buildEmptyResult() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", List.of());
        result.put("goodsAmount", "0.00");
        result.put("serviceAmount", "0.00");
        result.put("deliveryFee", "0.00");
        result.put("totalAmount", "0.00");
        result.put("deliveryCheck", null);
        return result;
    }
}
