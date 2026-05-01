package com.petshop.order.service;

import com.petshop.order.common.PageResult;
import com.petshop.order.entity.Member;

public interface MemberService {

    PageResult<Member> getList(int page, int size, String keyword, Long levelId);

    Member create(Member member);

    Member update(Long id, Member member);

    void delete(Long id);

    Member getMemberByPhone(String phone);
}
