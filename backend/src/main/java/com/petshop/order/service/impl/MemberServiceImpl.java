package com.petshop.order.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.petshop.order.common.BusinessException;
import com.petshop.order.common.PageResult;
import com.petshop.order.entity.Member;
import com.petshop.order.entity.MemberPhone;
import com.petshop.order.mapper.MemberMapper;
import com.petshop.order.mapper.MemberPhoneMapper;
import com.petshop.order.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final MemberPhoneMapper memberPhoneMapper;

    @Override
    public PageResult<Member> getList(int page, int size, String keyword, Long levelId) {
        PageHelper.startPage(page, size);
        List<Member> list = memberMapper.selectPageList(keyword, levelId);
        PageInfo<Member> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal(), page, size);
    }

    @Override
    @Transactional
    public Member create(Member member) {
        validatePhones(member.getPhones(), null);
        memberMapper.insert(member);
        insertPhones(member.getId(), member.getPhones());
        return memberMapper.selectById(member.getId());
    }

    @Override
    @Transactional
    public Member update(Long id, Member member) {
        Member existing = memberMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("会员不存在");
        }
        member.setId(id);
        memberMapper.updateById(member);
        if (member.getPhones() != null) {
            validatePhones(member.getPhones(), id);
            memberPhoneMapper.deleteByMemberId(id);
            insertPhones(id, member.getPhones());
        }
        return memberMapper.selectById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Member existing = memberMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("会员不存在");
        }
        memberPhoneMapper.deleteByMemberId(id);
        memberMapper.deleteById(id);
    }

    @Override
    public Member getMemberByPhone(String phone) {
        Long memberId = memberPhoneMapper.selectMemberIdByPhone(phone);
        if (memberId == null) {
            return null;
        }
        return memberMapper.selectById(memberId);
    }

    private void validatePhones(List<String> phones, Long excludeMemberId) {
        if (phones == null || phones.isEmpty()) {
            return;
        }
        for (String phone : phones) {
            int count = memberPhoneMapper.countByPhone(phone, excludeMemberId);
            if (count > 0) {
                throw new BusinessException("手机号 " + phone + " 已被其他会员使用");
            }
        }
    }

    private void insertPhones(Long memberId, List<String> phones) {
        if (phones == null || phones.isEmpty()) {
            return;
        }
        for (String phone : phones) {
            MemberPhone mp = new MemberPhone();
            mp.setMemberId(memberId);
            mp.setPhone(phone);
            memberPhoneMapper.insert(mp);
        }
    }
}
