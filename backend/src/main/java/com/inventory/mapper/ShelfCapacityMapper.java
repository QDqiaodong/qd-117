package com.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.entity.ShelfCapacity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ShelfCapacityMapper extends BaseMapper<ShelfCapacity> {

    IPage<ShelfCapacity> selectPageList(Page<ShelfCapacity> page,
                                        @Param("shelfNo") String shelfNo);
}
