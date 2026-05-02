package com.petshop.order.service;

import com.petshop.order.entity.AdminUser;

import java.util.List;

public interface AdminUserService {

    List<AdminUser> getList();

    AdminUser create(AdminUser user);

    AdminUser update(Long id, AdminUser user);

    void updateStatus(Long id, Integer status);

    void resetPassword(Long id, String newPassword);

    void delete(Long id);
}
