package com.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.entity.ScrapRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScrapRecordMapper extends BaseMapper<ScrapRecord> {

    IPage<ScrapRecord> selectPageList(Page<ScrapRecord> page,
                                       @Param("partModel") String partModel,
                                       @Param("scrapReason") String scrapReason,
                                       @Param("startTime") String startTime,
                                       @Param("endTime") String endTime);
}
