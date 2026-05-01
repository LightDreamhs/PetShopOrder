package com.petshop.order.service;

import com.petshop.order.entity.AppUser;

public interface AppAuthService {

    void sendSmsCode(String phone);

    AppUser login(String phone, String code);

    void logout();

    AppUser getCurrentUser();
}
