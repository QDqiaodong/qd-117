package com.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.entity.PinBox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PinBoxMapper extends BaseMapper<PinBox> {

    IPage<PinBox> selectPageList(Page<PinBox> page,
                                 @Param("partModel") String partModel,
                                 @Param("status") String status,
                                 @Param("boxNo") String boxNo,
                                 @Param("productionLine") String productionLine,
                                 @Param("startTime") String startTime,
                                 @Param("endTime") String endTime);

    List<PinBox> selectAvailableByPartId(@Param("partId") Long partId);

    List<PinBox> selectByBoxNos(@Param("boxNos") List<String> boxNos);

    List<PinBox> selectByStockInRecordId(@Param("stockInRecordId") Long stockInRecordId);

    List<PinBox> selectByStockOutRecordId(@Param("stockOutRecordId") Long stockOutRecordId);
}
