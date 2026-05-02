package com.petshop.order.controller.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.petshop.order.common.BusinessException;
import com.petshop.order.common.R;
import com.petshop.order.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        checkBossOrManager();
        return R.ok(statsService.getOverview());
    }

    @GetMapping("/orders")
    public R<List<Map<String, Object>>> orderTrends(@RequestParam(defaultValue = "DAY") String period) {
        checkBossOrManager();
        return R.ok(statsService.getOrderTrends(period));
    }

    @GetMapping("/members/ranking")
    public R<List<Map<String, Object>>> memberRanking(@RequestParam(required = false) Integer limit) {
        checkBossOrManager();
        return R.ok(statsService.getMemberRanking(limit));
    }

    private void checkBossOrManager() {
        if (!StpUtil.hasRole("BOSS") && !StpUtil.hasRole("MANAGER")) {
            throw new BusinessException("无权限访问");
        }
    }
}
