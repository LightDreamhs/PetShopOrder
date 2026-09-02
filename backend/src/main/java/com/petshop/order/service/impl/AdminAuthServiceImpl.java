package com.petshop.order.service.impl;

import com.petshop.order.config.StpAdminUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.petshop.order.common.BusinessException;
import com.petshop.order.entity.AdminUser;
import com.petshop.order.mapper.AdminUserMapper;
import com.petshop.order.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminUserMapper adminUserMapper;

    @Override
    public AdminUser login(String username, String password) {
        AdminUser user = adminUserMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }

        StpAdminUtil.login(user.getId(), "admin");
        // 员工归属店写入会话，ShopResolveInterceptor 据此限制数据范围。
        // 0=总部（session 底层是 ConcurrentHashMap，不接受 null）
        StpAdminUtil.getSession().set("shopId", user.getShopId() != null ? user.getShopId() : 0L);
        adminUserMapper.updateLastLoginTime(user.getId(), LocalDateTime.now());
        return user;
    }

    @Override
    public void logout() {
        StpAdminUtil.logout();
    }

    @Override
    public AdminUser getCurrentAdmin() {
        Long userId = StpAdminUtil.getLoginIdAsLong();
        AdminUser user = adminUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "管理员不存在");
        }
        return user;
    }
}
