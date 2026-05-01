package com.petshop.order.mapper;

import com.petshop.order.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {

    List<Category> selectList(@Param("type") String type);

    Category selectById(@Param("id") Long id);

    int insert(Category category);

    int updateById(Category category);

    int deleteById(@Param("id") Long id);

    int countProductsById(@Param("id") Long id);
}
