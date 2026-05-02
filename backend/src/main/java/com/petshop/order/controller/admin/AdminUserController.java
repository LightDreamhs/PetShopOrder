package com.petshop.order.controller.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.petshop.order.common.R;
import com.petshop.order.entity.AdminUser;
import com.petshop.order.service.AdminUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Map<String, String> ROLE_LABELS = Map.of(
            "BOSS", "老板",
            "MANAGER", "店长",
            "STAFF", "店员"
    );

    private final AdminUserService adminUserService;

    @GetMapping
    public R<List<Map<String, Object>>> list() {
        StpUtil.checkRole("BOSS");
        List<AdminUser> users = adminUserService.getList();
        List<Map<String, Object>> result = users.stream().map(this::toMap).toList();
        return R.ok(result);
    }

    @PostMapping
    public R<Map<String, Object>> create(@Valid @RequestBody CreateUserRequest req) {
        StpUtil.checkRole("BOSS");
        AdminUser user = new AdminUser();
        user.setUsername(req.getUsername());
        user.setPasswordHash(req.getPassword());
        user.setRealName(req.getRealName());
        user.setRole(req.getRole());
        AdminUser created = adminUserService.create(user);
        return R.ok(toMap(created));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        StpUtil.checkRole("BOSS");
        AdminUser user = new AdminUser();
        user.setRealName(req.getRealName());
        user.setRole(req.getRole());
        AdminUser updated = adminUserService.update(id, user);
        return R.ok(toMap(updated));
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        StpUtil.checkRole("BOSS");
        Integer status = "ENABLED".equals(req.getStatus()) ? 1 : 0;
        adminUserService.updateStatus(id, status);
        return R.ok();
    }

    @PutMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest req) {
        StpUtil.checkRole("BOSS");
        adminUserService.resetPassword(id, req.getNewPassword());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        StpUtil.checkRole("BOSS");
        adminUserService.delete(id);
        return R.ok();
    }

    private Map<String, Object> toMap(AdminUser u) {
        return Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "realName", u.getRealName() != null ? u.getRealName() : "",
                "role", u.getRole(),
                "roleLabel", ROLE_LABELS.getOrDefault(u.getRole(), ""),
                "status", u.getStatus() != null && u.getStatus() == 1 ? "ENABLED" : "DISABLED",
                "lastLoginTime", u.getLastLoginTime() != null ? u.getLastLoginTime().format(FMT) : ""
        );
    }

    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        @NotBlank(message = "姓名不能为空")
        private String realName;
        @NotBlank(message = "角色不能为空")
        private String role;
    }

    @Data
    public static class UpdateUserRequest {
        @NotBlank(message = "姓名不能为空")
        private String realName;
        @NotBlank(message = "角色不能为空")
        private String role;
    }

    @Data
    public static class StatusRequest {
        @NotBlank(message = "状态不能为空")
        private String status;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }
}
