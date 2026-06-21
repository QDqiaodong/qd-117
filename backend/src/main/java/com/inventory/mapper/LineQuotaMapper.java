package com.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.entity.LineQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LineQuotaMapper extends BaseMapper<LineQuota> {

    IPage<LineQuota> selectPageList(Page<LineQuota> page,
                                      @Param("quarter") String quarter,
                                      @Param("productionLine") String productionLine,
                                      @Param("partType") String partType);

    int consumeQuotaAtomic(@Param("id") Long id, @Param("quantity") int quantity);

    int rollbackQuotaAtomic(@Param("id") Long id, @Param("quantity") int quantity);

    int sumUsedFromRecords(@Param("productionLine") String productionLine,
                            @Param("partType") String partType,
                            @Param("startTime") String startTime,
                            @Param("endTime") String endTime);
}
