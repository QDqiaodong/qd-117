package com.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.dto.StockInDTO;
import com.inventory.entity.SmallPart;
import com.inventory.entity.StockInRecord;
import com.inventory.mapper.StockInRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockInService extends ServiceImpl<StockInRecordMapper, StockInRecord> {

    private final StockInRecordMapper stockInRecordMapper;
    private final SmallPartService smallPartService;

    public IPage<StockInRecord> getPageList(Integer pageNum, Integer pageSize, String partModel, String startTime, String endTime) {
        Page<StockInRecord> page = new Page<>(pageNum, pageSize);
        return stockInRecordMapper.selectPageList(page, partModel, startTime, endTime);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<StockInRecord> stockIn(StockInDTO dto) {
        List<StockInRecord> records = new ArrayList<>();
        for (StockInDTO.StockInItem item : dto.getItems()) {
            SmallPart part = smallPartService.getByModel(item.getPartModel());

            if (part == null) {
                part = new SmallPart();
                part.setPartModel(item.getPartModel());
                part.setPartName(item.getPartName() != null ? item.getPartName() : item.getPartModel());
                part.setPartType(item.getPartType());
                part.setSpecParams(item.getSpecParams());
                part.setShelfNo(item.getShelfNo());
                part.setStockQuantity(item.getQuantity());
                part.setUnit(item.getUnit() != null && !item.getUnit().isEmpty() ? item.getUnit() : "件");
                part.setRemark(item.getRemark());
                part = smallPartService.create(part);
            } else {
                if (item.getPartName() != null && !item.getPartName().isEmpty()) {
                    part.setPartName(item.getPartName());
                }
                if (item.getSpecParams() != null && !item.getSpecParams().isEmpty()) {
                    part.setSpecParams(item.getSpecParams());
                }
                if (item.getShelfNo() != null && !item.getShelfNo().isEmpty()) {
                    part.setShelfNo(item.getShelfNo());
                }
                if (item.getRemark() != null && !item.getRemark().isEmpty()) {
                    part.setRemark(item.getRemark());
                }
                smallPartService.update(part);
                smallPartService.increaseStock(part.getId(), item.getQuantity());
            }

            StockInRecord record = new StockInRecord();
            record.setPartId(part.getId());
            record.setPartModel(part.getPartModel());
            record.setQuantity(item.getQuantity());
            record.setShelfNo(item.getShelfNo());
            record.setOperator(dto.getOperator());
            record.setRemark(item.getRemark());
            record.setCreateTime(LocalDateTime.now());
            save(record);
            records.add(record);

            log.info("入库登记: 型号={}, 数量={}, 操作人={}", part.getPartModel(), item.getQuantity(), dto.getOperator());
        }
        return records;
    }
}
