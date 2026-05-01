package com.petshop.order.controller.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.petshop.order.common.R;
import com.petshop.order.entity.AdminUser;
import com.petshop.order.service.AdminAuthService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    private static final Map<String, String> ROLE_LABELS = Map.of(
            "BOSS", "老板",
            "MANAGER", "店长",
            "STAFF", "店员"
    );

    @PostMapping("/login")
    public R<Map<String, Object>> login(@Validated @RequestBody LoginRequest req) {
        AdminUser user = adminAuthService.login(req.getUsername(), req.getPassword());
        return R.ok(toProfileMap(user));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        adminAuthService.logout();
        return R.ok();
    }

    @GetMapping("/profile")
    public R<Map<String, Object>> profile() {
        AdminUser user = adminAuthService.getCurrentAdmin();
        return R.ok(toProfileMap(user));
    }

    private Map<String, Object> toProfileMap(AdminUser user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "realName", user.getRealName(),
                "role", user.getRole(),
                "roleLabel", ROLE_LABELS.getOrDefault(user.getRole(), user.getRole())
        );
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }
}
