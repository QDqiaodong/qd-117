package com.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.entity.LineReturnRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LineReturnRecordMapper extends BaseMapper<LineReturnRecord> {

    IPage<LineReturnRecord> selectPageList(Page<LineReturnRecord> page,
                                            @Param("partModel") String partModel,
                                            @Param("productionLine") String productionLine,
                                            @Param("reusableStatus") String reusableStatus,
                                            @Param("startTime") String startTime,
                                            @Param("endTime") String endTime);
}
