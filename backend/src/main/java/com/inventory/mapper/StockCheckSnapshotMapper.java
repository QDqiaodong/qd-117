package com.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.entity.StockCheckSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockCheckSnapshotMapper extends BaseMapper<StockCheckSnapshot> {

    List<StockCheckSnapshot> selectByQuarter(@Param("quarter") String quarter);

    StockCheckSnapshot selectByQuarterAndPartId(@Param("quarter") String quarter, @Param("partId") Long partId);

    int deleteByQuarter(@Param("quarter") String quarter);
}
