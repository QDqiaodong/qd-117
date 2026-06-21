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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapService extends ServiceImpl<ScrapRecordMapper, ScrapRecord> {

    private final ScrapRecordMapper scrapRecordMapper;
    private final SmallPartService smallPartService;
    private final ShelfCapacityService shelfCapacityService;

    public IPage<ScrapRecord> getPageList(Integer pageNum, Integer pageSize, String partModel,
                                               String scrapReason, String startTime, String endTime) {
        Page<ScrapRecord> page = new Page<>(pageNum, pageSize);
        return scrapRecordMapper.selectPageList(page, partModel, scrapReason, startTime, endTime);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<ScrapRecord> scrap(ScrapDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("报废明细不能为空");
        }
        if (dto.getOperator() != null) {
            dto.setOperator(dto.getOperator().trim());
        }
        if (dto.getRemark() != null) {
            dto.setRemark(dto.getRemark().trim());
        }
        for (ScrapDTO.ScrapItem item : dto.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("报废数量必须大于0，零件ID：" + item.getPartId());
            }
            if (item.getScrapReason() != null) {
                item.setScrapReason(item.getScrapReason().trim());
            }
        }

        List<ScrapRecord> records = new ArrayList<>();

        Map<Long, Integer> partTotalQtyMap = new HashMap<>();
        for (ScrapDTO.ScrapItem item : dto.getItems()) {
            partTotalQtyMap.merge(item.getPartId(), item.getQuantity(), Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : partTotalQtyMap.entrySet()) {
            SmallPart part = smallPartService.getById(entry.getKey());
            if (part.getStockQuantity() < entry.getValue()) {
                throw new BusinessException("库存不足，无法报废: " + part.getPartModel() +
                        ", 当前库存: " + part.getStockQuantity() +
                        ", 报废合计: " + entry.getValue());
            }
        }

        Map<Long, SmallPart> partCache = new HashMap<>();
        for (ScrapDTO.ScrapItem item : dto.getItems()) {
            SmallPart part = partCache.computeIfAbsent(item.getPartId(), smallPartService::getById);

            smallPartService.decreaseStock(part.getId(), item.getQuantity());

            if ("顶针".equals(part.getPartType())) {
                shelfCapacityService.decreasePinBoxes(part.getShelfNo(), item.getQuantity());
            } else if ("限位垫片".equals(part.getPartType())) {
                shelfCapacityService.decreaseShimPacks(part.getShelfNo(), item.getQuantity());
            }

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

            log.info("报废登记: 型号={}, 数量={}, 原因={}, 货架={}, 操作人={}",
                    part.getPartModel(), item.getQuantity(), item.getScrapReason(),
                    part.getShelfNo(), dto.getOperator());
        }
        return records;
    }
}
