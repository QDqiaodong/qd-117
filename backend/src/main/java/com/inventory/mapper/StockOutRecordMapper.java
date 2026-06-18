package com.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.entity.StockOutRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockOutRecordMapper extends BaseMapper<StockOutRecord> {

    IPage<StockOutRecord> selectPageList(Page<StockOutRecord> page,
                                          @Param("partModel") String partModel,
                                          @Param("productionLine") String productionLine,
                                          @Param("startTime") String startTime,
                                          @Param("endTime") String endTime);
}
