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

    int increasePinBoxesAtomic(@Param("id") Long id, @Param("quantity") int quantity);

    int increaseShimPacksAtomic(@Param("id") Long id, @Param("quantity") int quantity);

    int decreasePinBoxesAtomic(@Param("id") Long id, @Param("quantity") int quantity);

    int decreaseShimPacksAtomic(@Param("id") Long id, @Param("quantity") int quantity);
}
