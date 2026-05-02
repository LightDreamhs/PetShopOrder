package com.petshop.order.service.impl;

import com.petshop.order.entity.SystemConfigDeliveryTier;
import com.petshop.order.mapper.SystemConfigDeliveryTierMapper;
import com.petshop.order.service.DeliveryService;
import com.petshop.order.service.SystemConfigService;
import com.petshop.order.service.dto.DeliveryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final SystemConfigService systemConfigService;
    private final SystemConfigDeliveryTierMapper deliveryTierMapper;

    @Override
    public Map<String, Object> checkDelivery(List<DeliveryItem> items, String lat, String lng) {
        Map<String, Object> shopLocation = systemConfigService.getShopLocation();
        Map<String, Object> deliveryConfig = systemConfigService.getDeliveryConfig();

        BigDecimal shopLat = (BigDecimal) shopLocation.get("shopLat");
        BigDecimal shopLng = (BigDecimal) shopLocation.get("shopLng");
        BigDecimal deliveryRadiusKm = (BigDecimal) deliveryConfig.get("deliveryRadiusKm");
        BigDecimal deliveryMinAmount = (BigDecimal) deliveryConfig.get("deliveryMinAmount");
        String deliveryFeeType = (String) deliveryConfig.get("deliveryFeeType");

        boolean hasDeliverableGoods = items.stream()
                .anyMatch(item -> "GOODS".equals(item.getType())
                        && Boolean.TRUE.equals(item.getSupportDelivery()));

        BigDecimal deliverableGoodsOriginal = items.stream()
                .filter(item -> "GOODS".equals(item.getType())
                        && Boolean.TRUE.equals(item.getSupportDelivery()))
                .map(item -> item.getOriginalPrice().multiply(new BigDecimal(item.getQuantity().toString())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean canDeliver = hasDeliverableGoods;
        boolean reachedMinAmount = deliveryMinAmount != null
                && deliverableGoodsOriginal.compareTo(deliveryMinAmount) >= 0;
        BigDecimal gap = BigDecimal.ZERO;
        if (!reachedMinAmount && deliveryMinAmount != null) {
            gap = deliveryMinAmount.subtract(deliverableGoodsOriginal);
        }

        boolean withinRadius = true;
        Long distanceMeter = null;
        BigDecimal distanceKm = null;
        String deliveryDistanceText = null;

        if (lat != null && !lat.isEmpty() && lng != null && !lng.isEmpty()) {
            double userLat = Double.parseDouble(lat);
            double userLng = Double.parseDouble(lng);
            double shopLatDouble = shopLat.doubleValue();
            double shopLngDouble = shopLng.doubleValue();

            double calculatedDistanceKm = haversineKm(shopLatDouble, shopLngDouble, userLat, userLng);
            double calculatedDistanceMeter = calculatedDistanceKm * 1000;

            distanceMeter = Math.round(calculatedDistanceMeter);
            distanceKm = new BigDecimal(calculatedDistanceKm).setScale(2, RoundingMode.HALF_UP);
            deliveryDistanceText = formatDistance(calculatedDistanceMeter);

            if (deliveryRadiusKm != null && distanceKm.compareTo(deliveryRadiusKm) > 0) {
                withinRadius = false;
                canDeliver = false;
            }
        }

        BigDecimal deliveryFee = BigDecimal.ZERO;
        if (canDeliver && withinRadius && "FREE".equals(deliveryFeeType)) {
            deliveryFee = BigDecimal.ZERO;
        } else if (canDeliver && withinRadius && "TIERED".equals(deliveryFeeType) && distanceKm != null) {
            deliveryFee = calculateDeliveryFee(distanceKm, 1L);
            if (deliveryFee == null) {
                canDeliver = false;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("canDeliver", canDeliver);
        result.put("deliverableGoodsOriginal", deliverableGoodsOriginal.toPlainString());
        result.put("minAmount", deliveryMinAmount != null ? deliveryMinAmount.toPlainString() : "0.00");
        result.put("reachedMinAmount", reachedMinAmount);
        result.put("gap", reachedMinAmount ? null : gap.toPlainString());
        result.put("deliveryDistanceMeter", distanceMeter);
        result.put("deliveryDistanceText", deliveryDistanceText);
        result.put("deliveryFee", deliveryFee != null ? deliveryFee.toPlainString() : null);
        return result;
    }

    @Override
    public BigDecimal calculateDeliveryFee(BigDecimal distanceKm, Long configId) {
        List<SystemConfigDeliveryTier> tiers = deliveryTierMapper.selectByConfigId(configId);
        for (SystemConfigDeliveryTier tier : tiers) {
            int minCmp = distanceKm.compareTo(tier.getMinDistanceKm());
            int maxCmp = distanceKm.compareTo(tier.getMaxDistanceKm());
            if (minCmp >= 0 && maxCmp < 0) {
                return tier.getFee();
            }
        }
        return null;
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c;
    }

    private String formatDistance(double distanceMeter) {
        if (distanceMeter < 1000) {
            return Math.round(distanceMeter) + "m";
        }
        double km = distanceMeter / 1000.0;
        if (km == Math.floor(km)) {
            return (long) km + "km";
        }
        return String.format("%.1fkm", km);
    }
}
