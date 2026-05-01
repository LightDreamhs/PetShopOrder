package com.petshop.order.service;

import com.petshop.order.entity.AppUser;

import java.util.Map;

public interface AppAuthService {

    void sendSmsCode(String phone);

    Map<String, Object> login(String phone, String code);

    void logout();

    AppUser getCurrentUser();
}
