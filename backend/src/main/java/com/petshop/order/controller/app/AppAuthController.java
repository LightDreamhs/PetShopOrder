package com.petshop.order.controller.app;

import cn.dev33.satoken.stp.StpUtil;
import com.petshop.order.common.R;
import com.petshop.order.entity.AppUser;
import com.petshop.order.service.AppAuthService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/app/auth")
@RequiredArgsConstructor
public class AppAuthController {

    private final AppAuthService appAuthService;

    @PostMapping("/sms-code")
    public R<Void> sendSmsCode(@Validated @RequestBody SmsCodeRequest req) {
        appAuthService.sendSmsCode(req.getPhone());
        return R.ok();
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@Validated @RequestBody LoginRequest req) {
        Map<String, Object> loginResult = appAuthService.login(req.getPhone(), req.getCode());
        AppUser user = (AppUser) loginResult.get("user");
        boolean isNew = (boolean) loginResult.get("isNew");

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("phone", maskPhone(user.getPhone()));
        result.put("isNew", isNew);
        return R.ok(result);
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        appAuthService.logout();
        return R.ok();
    }

    @GetMapping("/check")
    public R<Map<String, Object>> check() {
        boolean loggedIn = StpUtil.isLogin();
        String phone = null;
        if (loggedIn) {
            AppUser user = appAuthService.getCurrentUser();
            phone = maskPhone(user.getPhone());
        }
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("loggedIn", loggedIn);
        result.put("phone", phone);
        return R.ok(result);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    @Data
    public static class SmsCodeRequest {
        @NotBlank @Pattern(regexp = "^1\\d{10}$")
        private String phone;
    }

    @Data
    public static class LoginRequest {
        @NotBlank @Pattern(regexp = "^1\\d{10}$")
        private String phone;
        @NotBlank
        private String code;
    }
}
