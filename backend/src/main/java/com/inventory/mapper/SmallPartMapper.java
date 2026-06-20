package com.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.entity.SmallPart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SmallPartMapper extends BaseMapper<SmallPart> {

    IPage<SmallPart> selectPageList(Page<SmallPart> page,
                                     @Param("partType") String partType,
                                     @Param("keyword") String keyword);

    int decreaseStockAtomic(@Param("id") Long id, @Param("quantity") Integer quantity);

    int increaseStockAtomic(@Param("id") Long id, @Param("quantity") Integer quantity);
}
