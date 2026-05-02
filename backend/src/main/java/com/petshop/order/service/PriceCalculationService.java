package com.petshop.order.service;

import com.petshop.order.service.dto.CalculatedItemResult;
import com.petshop.order.service.dto.CartItemInput;

import java.util.List;
import java.util.Map;

public interface PriceCalculationService {

    Map<String, Object> calculateItems(List<CartItemInput> items, String userPhone);
}
