package com.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.entity.ScrapReason;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScrapReasonMapper extends BaseMapper<ScrapReason> {

    IPage<ScrapReason> selectPageList(Page<ScrapReason> page,
                                   @Param("reasonName") String reasonName,
                                   @Param("partType") String partType,
                                   @Param("status") Integer status);
}
