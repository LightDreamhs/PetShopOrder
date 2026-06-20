package com.petshop.order.service;

import com.petshop.order.entity.UserAddress;

import java.util.List;

public interface UserAddressService {

    List<UserAddress> listByUser(Long userId);

    UserAddress create(Long userId, UserAddress userAddress);

    UserAddress update(Long userId, Long id, UserAddress userAddress);

    void setDefault(Long userId, Long id);

    void delete(Long userId, Long id);
}
