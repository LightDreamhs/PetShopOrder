package com.petshop.order.service.impl;

import com.petshop.order.common.ShopContext;
import com.petshop.order.mapper.StatsMapper;
import com.petshop.order.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsMapper statsMapper;

    @Override
    public Map<String, Object> getOverview() {
        // 统计口径：当前请求上下文的门店
        Long shopId = ShopContext.require();
        Map<String, Object> result = statsMapper.selectOverview(shopId);
        if (result.get("todayAmount") != null) {
            result.put("todayAmount", result.get("todayAmount").toString());
        }
        if (result.get("totalAmount") != null) {
            result.put("totalAmount", result.get("totalAmount").toString());
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getOrderTrends(String period) {
        if (!"DAY".equals(period) && !"WEEK".equals(period) && !"MONTH".equals(period)) {
            throw new com.petshop.order.common.BusinessException("period 参数只支持 DAY、WEEK、MONTH");
        }
        List<Map<String, Object>> trends = statsMapper.selectOrderTrends(period, ShopContext.require());
        for (Map<String, Object> item : trends) {
            if (item.get("amount") != null) {
                item.put("amount", item.get("amount").toString());
            }
        }
        return trends;
    }

    @Override
    public List<Map<String, Object>> getMemberRanking(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        List<Map<String, Object>> ranking = statsMapper.selectMemberRanking(limit, ShopContext.require());
        for (Map<String, Object> item : ranking) {
            if (item.get("totalAmount") != null) {
                item.put("totalAmount", item.get("totalAmount").toString());
            }
            if (item.get("phone") == null) {
                item.put("phone", "");
            }
            if (item.get("levelName") == null) {
                item.put("levelName", "");
            }
        }
        return ranking;
    }
}
