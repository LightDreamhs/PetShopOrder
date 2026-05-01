package com.petshop.order.service;

import com.petshop.order.entity.MemberLevel;

import java.util.List;

public interface MemberLevelService {

    List<MemberLevel> getList();

    MemberLevel create(MemberLevel memberLevel);

    MemberLevel update(Long id, MemberLevel memberLevel);

    void updateStatus(Long id, Integer status);

    void delete(Long id);
}
