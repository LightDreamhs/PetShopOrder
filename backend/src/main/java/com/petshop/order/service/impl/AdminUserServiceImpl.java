package com.petshop.order.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.petshop.order.common.BusinessException;
import com.petshop.order.entity.AdminUser;
import com.petshop.order.mapper.AdminUserMapper;
import com.petshop.order.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserMapper adminUserMapper;

    private static final Set<String> ALLOWED_ROLES = Set.of("MANAGER", "STAFF");

    @Override
    public List<AdminUser> getList() {
        return adminUserMapper.selectAll();
    }

    @Override
    public AdminUser create(AdminUser user) {
        if (!ALLOWED_ROLES.contains(user.getRole())) {
            throw new BusinessException("新建管理员角色只能是 MANAGER 或 STAFF");
        }
        AdminUser existing = adminUserMapper.selectByUsername(user.getUsername());
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }
        user.setPasswordHash(BCrypt.hashpw(user.getPasswordHash()));
        user.setStatus(1);
        adminUserMapper.insert(user);
        return adminUserMapper.selectById(user.getId());
    }

    @Override
    public AdminUser update(Long id, AdminUser user) {
        AdminUser existing = adminUserMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("管理员不存在");
        }
        user.setId(id);
        adminUserMapper.updateById(user);
        return adminUserMapper.selectById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        AdminUser existing = adminUserMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("管理员不存在");
        }
        if ("BOSS".equals(existing.getRole())) {
            throw new BusinessException("BOSS 账号不可被禁用");
        }
        adminUserMapper.updateStatus(id, status);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        AdminUser existing = adminUserMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("管理员不存在");
        }
        adminUserMapper.updatePassword(id, BCrypt.hashpw(newPassword));
    }

    @Override
    public void delete(Long id) {
        AdminUser existing = adminUserMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("管理员不存在");
        }
        if ("BOSS".equals(existing.getRole())) {
            throw new BusinessException("BOSS 账号不可删除");
        }
        adminUserMapper.deleteById(id);
    }
}
