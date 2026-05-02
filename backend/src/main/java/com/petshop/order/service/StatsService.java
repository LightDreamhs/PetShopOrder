package com.petshop.order.service;

import java.util.List;
import java.util.Map;

public interface StatsService {

    Map<String, Object> getOverview();

    List<Map<String, Object>> getOrderTrends(String period);

    List<Map<String, Object>> getMemberRanking(Integer limit);
}
