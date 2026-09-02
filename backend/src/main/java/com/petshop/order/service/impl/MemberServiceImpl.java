package com.petshop.order.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.petshop.order.common.BusinessException;
import com.petshop.order.common.PageResult;
import com.petshop.order.common.ShopContext;
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
        List<Member> list = memberMapper.selectPageList(keyword, levelId, ShopContext.require());
        PageInfo<Member> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal(), page, size);
    }

    @Override
    @Transactional
    public Member create(Member member) {
        Long shopId = ShopContext.require();
        validatePhones(member.getPhones(), null, shopId);
        member.setShopId(shopId);
        memberMapper.insert(member);
        insertPhones(shopId, member.getId(), member.getPhones());
        return memberMapper.selectById(member.getId());
    }

    @Override
    @Transactional
    public Member update(Long id, Member member) {
        Long shopId = ShopContext.require();
        Member existing = memberMapper.selectById(id);
        if (existing == null || !shopId.equals(existing.getShopId())) {
            throw new BusinessException("会员不存在");
        }
        member.setId(id);
        memberMapper.updateById(member);
        if (member.getPhones() != null) {
            validatePhones(member.getPhones(), id, shopId);
            memberPhoneMapper.deleteByMemberId(id);
            insertPhones(shopId, id, member.getPhones());
        }
        return memberMapper.selectById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Member existing = memberMapper.selectById(id);
        if (existing == null || !ShopContext.require().equals(existing.getShopId())) {
            throw new BusinessException("会员不存在");
        }
        memberPhoneMapper.deleteByMemberId(id);
        memberMapper.deleteById(id);
    }

    @Override
    public Member getMemberByPhone(String phone) {
        Long memberId = memberPhoneMapper.selectMemberIdByShopAndPhone(ShopContext.require(), phone);
        if (memberId == null) {
            return null;
        }
        return memberMapper.selectById(memberId);
    }

    private void validatePhones(List<String> phones, Long excludeMemberId, Long shopId) {
        if (phones == null || phones.isEmpty()) {
            return;
        }
        for (String phone : phones) {
            int count = memberPhoneMapper.countByPhone(phone, excludeMemberId, shopId);
            if (count > 0) {
                throw new BusinessException("手机号 " + phone + " 已被本店其他会员使用");
            }
        }
    }

    private void insertPhones(Long shopId, Long memberId, List<String> phones) {
        if (phones == null || phones.isEmpty()) {
            return;
        }
        for (String phone : phones) {
            MemberPhone mp = new MemberPhone();
            mp.setShopId(shopId);
            mp.setMemberId(memberId);
            mp.setPhone(phone);
            memberPhoneMapper.insert(mp);
        }
    }
}
