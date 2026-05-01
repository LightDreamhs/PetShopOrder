package com.petshop.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
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

        StpUtil.login(user.getId(), "admin");
        adminUserMapper.updateLastLoginTime(user.getId(), LocalDateTime.now());
        return user;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public AdminUser getCurrentAdmin() {
        Long userId = StpUtil.getLoginIdAsLong();
        AdminUser user = adminUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "管理员不存在");
        }
        return user;
    }
}
