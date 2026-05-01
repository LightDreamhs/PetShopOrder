package com.petshop.order.service;

import com.petshop.order.entity.AdminUser;

public interface AdminAuthService {

    AdminUser login(String username, String password);

    void logout();

    AdminUser getCurrentAdmin();
}
