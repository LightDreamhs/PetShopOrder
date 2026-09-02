package com.petshop.order.service;

import com.petshop.order.service.dto.DeliveryItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface DeliveryService {

    Map<String, Object> checkDelivery(List<DeliveryItem> items, String lat, String lng);

    /** 按店铺查分段运费；距离不在任何分段内返回 null */
    BigDecimal calculateDeliveryFee(BigDecimal distanceKm, Long shopId);
}
