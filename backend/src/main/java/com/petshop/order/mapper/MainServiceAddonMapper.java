package com.petshop.order.mapper;

import com.petshop.order.entity.MainServiceAddon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MainServiceAddonMapper {

    /** 查某主服务绑定的附加服务（含 product + 默认 sku 信息），按 sort 排序 */
    List<Map<String, Object>> selectAddonsByMainProduct(@Param("mainProductId") Long mainProductId);

    /** 查某主服务已绑定的附加服务 id 列表（用于 Admin 编辑回显） */
    List<Long> selectAddonIdsByMainProductId(@Param("mainProductId") Long mainProductId);

    /** 删除某主服务的全部绑定（覆盖式更新前调用） */
    int deleteByMainProductId(@Param("mainProductId") Long mainProductId);

    /** 批量插入绑定（覆盖式更新时，先 deleteByMainProductId 再调此方法） */
    int insertBatch(@Param("mainProductId") Long mainProductId,
                    @Param("addonProductIds") List<Long> addonProductIds);

    int insert(MainServiceAddon mainServiceAddon);

    int deleteByBoth(@Param("mainProductId") Long mainProductId,
                     @Param("addonProductId") Long addonProductId);

    int existsByBoth(@Param("mainProductId") Long mainProductId,
                     @Param("addonProductId") Long addonProductId);
}
