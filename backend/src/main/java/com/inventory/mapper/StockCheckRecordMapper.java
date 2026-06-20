package com.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.dto.StockCheckHotZoneVO;
import com.inventory.entity.StockCheckRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface StockCheckRecordMapper extends BaseMapper<StockCheckRecord> {

    IPage<StockCheckRecord> selectPageList(Page<StockCheckRecord> page,
                                            @Param("partModel") String partModel,
                                            @Param("quarter") String quarter,
                                            @Param("startTime") String startTime,
                                            @Param("endTime") String endTime,
                                            @Param("confirmStatus") Integer confirmStatus);

    List<StockCheckHotZoneVO.HotZoneRow> selectHotZoneRows(@Param("quarter") String quarter);

    List<StockCheckRecord> selectByQuarterAndPartModels(@Param("quarter") String quarter,
                                                        @Param("partModels") List<String> partModels);

    int updateConfirm(@Param("id") Long id,
                      @Param("handleConclusion") String handleConclusion,
                      @Param("confirmPerson") String confirmPerson,
                      @Param("confirmTime") LocalDateTime confirmTime);
}

