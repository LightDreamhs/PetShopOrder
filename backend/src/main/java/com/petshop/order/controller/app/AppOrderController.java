package com.petshop.order.controller.app;

import com.petshop.order.common.PageResult;
import com.petshop.order.common.R;
import com.petshop.order.entity.AppUser;
import com.petshop.order.service.AppAuthService;
import com.petshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/app/orders")
@RequiredArgsConstructor
public class AppOrderController {

    private final OrderService orderService;
    private final AppAuthService appAuthService;

    @PostMapping
    public R<Map<String, Object>> createOrder(@RequestBody Map<String, Object> req) {
        AppUser currentUser = appAuthService.getCurrentUser();
        req.put("_userId", currentUser.getId());
        return R.ok(orderService.createOrder(req));
    }

    @GetMapping
    public R<PageResult<Map<String, Object>>> getMyOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        AppUser currentUser = appAuthService.getCurrentUser();
        return R.ok(orderService.getMyOrders(currentUser.getId(), page, size));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> getOrderDetail(@PathVariable Long id) {
        AppUser currentUser = appAuthService.getCurrentUser();
        return R.ok(orderService.getOrderDetail(id, currentUser.getId()));
    }
}
