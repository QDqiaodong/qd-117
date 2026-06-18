package com.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.dto.StockCheckDTO;
import com.inventory.entity.SmallPart;
import com.inventory.entity.StockCheckRecord;
import com.inventory.mapper.StockCheckRecordMapper;
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
public class StockCheckService extends ServiceImpl<StockCheckRecordMapper, StockCheckRecord> {

    private final StockCheckRecordMapper stockCheckRecordMapper;
    private final SmallPartService smallPartService;

    public IPage<StockCheckRecord> getPageList(Integer pageNum, Integer pageSize, String partModel,
                                                String quarter, String startTime, String endTime) {
        Page<StockCheckRecord> page = new Page<>(pageNum, pageSize);
        return stockCheckRecordMapper.selectPageList(page, partModel, quarter, startTime, endTime);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<StockCheckRecord> checkStock(StockCheckDTO dto) {
        List<StockCheckRecord> records = new ArrayList<>();

        for (StockCheckDTO.StockCheckItem item : dto.getItems()) {
            SmallPart part = smallPartService.getById(item.getPartId());
            int systemQuantity = part.getStockQuantity();
            int actualQuantity = item.getActualQuantity();
            int diffQuantity = actualQuantity - systemQuantity;

            StockCheckRecord record = new StockCheckRecord();
            record.setPartId(part.getId());
            record.setPartModel(part.getPartModel());
            record.setSystemQuantity(systemQuantity);
            record.setActualQuantity(actualQuantity);
            record.setDiffQuantity(diffQuantity);
            record.setShelfNo(part.getShelfNo());
            record.setCheckPerson(dto.getCheckPerson());
            record.setRemark(item.getRemark());
            record.setQuarter(dto.getQuarter());
            record.setCreateTime(LocalDateTime.now());
            save(record);
            records.add(record);

            if (diffQuantity != 0) {
                log.warn("库存差异: 型号={}, 系统库存={}, 实际库存={}, 差异={}",
                        part.getPartModel(), systemQuantity, actualQuantity, diffQuantity);
            } else {
                log.info("盘点一致: 型号={}, 库存数量={}", part.getPartModel(), actualQuantity);
            }
        }
        return records;
    }
}
