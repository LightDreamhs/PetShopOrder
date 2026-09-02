package com.petshop.order.service.impl;

import com.petshop.order.common.BusinessException;
import com.petshop.order.common.ShopContext;
import com.petshop.order.entity.MemberLevel;
import com.petshop.order.mapper.MemberLevelMapper;
import com.petshop.order.service.MemberLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberLevelServiceImpl implements MemberLevelService {

    private final MemberLevelMapper memberLevelMapper;

    @Override
    public List<MemberLevel> getList() {
        return memberLevelMapper.selectList(ShopContext.require());
    }

    @Override
    public MemberLevel create(MemberLevel memberLevel) {
        memberLevel.setShopId(ShopContext.require());
        memberLevel.setStatus(1);
        memberLevelMapper.insert(memberLevel);
        return memberLevelMapper.selectById(memberLevel.getId());
    }

    @Override
    public MemberLevel update(Long id, MemberLevel memberLevel) {
        MemberLevel existing = requireShopLevel(id);
        memberLevel.setId(id);
        memberLevelMapper.updateById(memberLevel);
        return memberLevelMapper.selectById(existing.getId());
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        requireShopLevel(id);
        memberLevelMapper.updateStatus(id, status);
    }

    @Override
    public void delete(Long id) {
        requireShopLevel(id);
        int count = memberLevelMapper.countByLevelId(id);
        if (count > 0) {
            throw new BusinessException("该等级下存在会员，无法删除");
        }
        memberLevelMapper.deleteById(id);
    }

    /** 取等级并校验归属当前店（店长/店员不可跨店操作） */
    private MemberLevel requireShopLevel(Long id) {
        MemberLevel existing = memberLevelMapper.selectById(id);
        if (existing == null || !ShopContext.require().equals(existing.getShopId())) {
            throw new BusinessException("会员等级不存在");
        }
        return existing;
    }
}
