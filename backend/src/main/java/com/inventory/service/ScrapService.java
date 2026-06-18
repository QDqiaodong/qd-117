package com.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.dto.ScrapDTO;
import com.inventory.entity.ScrapRecord;
import com.inventory.entity.SmallPart;
import com.inventory.exception.BusinessException;
import com.inventory.mapper.ScrapRecordMapper;
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
public class ScrapService extends ServiceImpl<ScrapRecordMapper, ScrapRecord> {

    private final ScrapRecordMapper scrapRecordMapper;
    private final SmallPartService smallPartService;

    public IPage<ScrapRecord> getPageList(Integer pageNum, Integer pageSize, String partModel,
                                               String scrapReason, String startTime, String endTime) {
        Page<ScrapRecord> page = new Page<>(pageNum, pageSize);
        return scrapRecordMapper.selectPageList(page, partModel, scrapReason, startTime, endTime);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<ScrapRecord> scrap(ScrapDTO dto) {
        List<ScrapRecord> records = new ArrayList<>();

        for (ScrapDTO.ScrapItem item : dto.getItems()) {
            SmallPart part = smallPartService.getById(item.getPartId());
            if (part.getStockQuantity() < item.getQuantity()) {
                throw new BusinessException("库存不足，无法报废: " + part.getPartModel() +
                        ", 当前库存: " + part.getStockQuantity() +
                        ", 报废数量: " + item.getQuantity());
            }
        }

        for (ScrapDTO.ScrapItem item : dto.getItems()) {
            SmallPart part = smallPartService.getById(item.getPartId());

            smallPartService.decreaseStock(part.getId(), item.getQuantity());

            ScrapRecord record = new ScrapRecord();
            record.setPartId(part.getId());
            record.setPartModel(part.getPartModel());
            record.setQuantity(item.getQuantity());
            record.setScrapReason(item.getScrapReason());
            record.setOperator(dto.getOperator());
            record.setRemark(dto.getRemark());
            record.setCreateTime(LocalDateTime.now());
            save(record);
            records.add(record);

            log.info("报废登记: 型号={}, 数量={}, 原因={}, 操作人={}",
                    part.getPartModel(), item.getQuantity(), item.getScrapReason(), dto.getOperator());
        }
        return records;
    }
}
