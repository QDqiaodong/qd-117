package com.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.entity.StockInRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockInRecordMapper extends BaseMapper<StockInRecord> {

    IPage<StockInRecord> selectPageList(Page<StockInRecord> page,
                                         @Param("partModel") String partModel,
                                         @Param("startTime") String startTime,
                                         @Param("endTime") String endTime);
}
