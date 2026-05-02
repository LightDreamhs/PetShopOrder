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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        String deliveryLat = (String) body.get("deliveryLat");
        String deliveryLng = (String) body.get("deliveryLng");

        List<CartItemInput> cartItems = new ArrayList<>();
        List<DeliveryItem> deliveryItems = new ArrayList<>();

        for (Map<String, Object> item : inputItems) {
            CartItemInput cartItem = new CartItemInput();
            cartItem.setProductId(toLong(item.get("productId")));
            cartItem.setSkuId(toLong(item.get("skuId")));
            cartItem.setQuantity(toInteger(item.get("quantity")));
            cartItems.add(cartItem);

            Product product = productMapper.selectById(cartItem.getProductId());
            if (product != null) {
                List<Sku> skus = skuMapper.selectByProductId(cartItem.getProductId());
                Sku sku = skus.stream()
                        .filter(s -> s.getId().equals(cartItem.getSkuId()))
                        .findFirst().orElse(null);

                DeliveryItem deliveryItem = new DeliveryItem();
                deliveryItem.setProductId(cartItem.getProductId());
                deliveryItem.setSkuId(cartItem.getSkuId());
                deliveryItem.setQuantity(cartItem.getQuantity());
                deliveryItem.setType(product.getType());
                deliveryItem.setOriginalPrice(sku != null ? sku.getPrice() : BigDecimal.ZERO);
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
}
