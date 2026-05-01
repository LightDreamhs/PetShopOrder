package com.petshop.order.mapper;

import com.petshop.order.entity.MemberLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberLevelMapper {

    List<MemberLevel> selectList();

    MemberLevel selectById(@Param("id") Long id);

    int insert(MemberLevel memberLevel);

    int updateById(MemberLevel memberLevel);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int deleteById(@Param("id") Long id);

    int countByLevelId(@Param("levelId") Long levelId);
}
