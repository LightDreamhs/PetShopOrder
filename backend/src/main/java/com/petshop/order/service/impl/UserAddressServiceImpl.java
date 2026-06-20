package com.petshop.order.service.impl;

import com.petshop.order.common.BusinessException;
import com.petshop.order.entity.UserAddress;
import com.petshop.order.mapper.UserAddressMapper;
import com.petshop.order.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private static final int MAX_ADDRESS_PER_USER = 10;

    private final UserAddressMapper userAddressMapper;

    @Override
    public List<UserAddress> listByUser(Long userId) {
        return userAddressMapper.selectByUserId(userId);
    }

    @Override
    @Transactional
    public UserAddress create(Long userId, UserAddress userAddress) {
        int count = userAddressMapper.countByUserId(userId);
        if (count >= MAX_ADDRESS_PER_USER) {
            throw new BusinessException("最多只能保存 " + MAX_ADDRESS_PER_USER + " 个地址");
        }
        userAddress.setUserId(userId);
        userAddress.setIsDefault(0);
        // 第一个地址自动设为默认
        if (count == 0) {
            userAddress.setIsDefault(1);
        }
        userAddressMapper.insert(userAddress);
        return userAddressMapper.selectById(userAddress.getId());
    }

    @Override
    public UserAddress update(Long userId, Long id, UserAddress userAddress) {
        UserAddress existing = userAddressMapper.selectById(id);
        if (existing == null || !userId.equals(existing.getUserId())) {
            throw new BusinessException("地址不存在");
        }
        userAddress.setId(id);
        userAddressMapper.updateById(userAddress);
        return userAddressMapper.selectById(id);
    }

    @Override
    @Transactional
    public void setDefault(Long userId, Long id) {
        UserAddress existing = userAddressMapper.selectById(id);
        if (existing == null || !userId.equals(existing.getUserId())) {
            throw new BusinessException("地址不存在");
        }
        userAddressMapper.clearDefaultByUserId(userId);
        userAddressMapper.setDefault(id);
    }

    @Override
    public void delete(Long userId, Long id) {
        UserAddress existing = userAddressMapper.selectById(id);
        if (existing == null || !userId.equals(existing.getUserId())) {
            throw new BusinessException("地址不存在");
        }
        userAddressMapper.deleteById(id);
    }
}
