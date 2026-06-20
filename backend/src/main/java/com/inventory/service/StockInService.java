package com.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.dto.StockInDTO;
import com.inventory.dto.StockInValidationVO;
import com.inventory.entity.SmallPart;
import com.inventory.entity.StockInRecord;
import com.inventory.exception.BusinessException;
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
    private final ShelfCapacityService shelfCapacityService;

    public IPage<StockInRecord> getPageList(Integer pageNum, Integer pageSize, String partModel, String startTime, String endTime) {
        Page<StockInRecord> page = new Page<>(pageNum, pageSize);
        return stockInRecordMapper.selectPageList(page, partModel, startTime, endTime);
    }

    public StockInValidationVO validate(StockInDTO dto) {
        return shelfCapacityService.validateStockIn(dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<StockInRecord> stockIn(StockInDTO dto) {
        StockInValidationVO validation = validate(dto);
        if (!validation.isValid()) {
            String errorMsg = String.join("; ", validation.getErrors());
            throw new BusinessException(errorMsg);
        }

        List<StockInRecord> records = new ArrayList<>();
        for (StockInDTO.StockInItem item : dto.getItems()) {
            SmallPart existingPart = smallPartService.getByModel(item.getPartModel());
            String oldShelfNo = existingPart != null ? existingPart.getShelfNo() : null;
            boolean isNewPart = existingPart == null;
            boolean shelfChanged = existingPart != null && item.getShelfNo() != null
                    && !item.getShelfNo().isEmpty() && !item.getShelfNo().equals(existingPart.getShelfNo());

            SmallPart part;
            if (isNewPart) {
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
                part = existingPart;
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

            if ("顶针".equals(item.getPartType())) {
                if (shelfChanged && oldShelfNo != null) {
                    int oldQty = existingPart != null && existingPart.getStockQuantity() != null
                            ? existingPart.getStockQuantity() : 0;
                    if (oldQty > 0) {
                        shelfCapacityService.decreasePinBoxes(oldShelfNo, oldQty);
                    }
                    shelfCapacityService.increasePinBoxes(item.getShelfNo(), oldQty + item.getQuantity());
                } else {
                    shelfCapacityService.increasePinBoxes(item.getShelfNo(), item.getQuantity());
                }
            } else if ("限位垫片".equals(item.getPartType())) {
                if (shelfChanged && oldShelfNo != null) {
                    int oldQty = existingPart != null && existingPart.getStockQuantity() != null
                            ? existingPart.getStockQuantity() : 0;
                    if (oldQty > 0) {
                        shelfCapacityService.decreaseShimPacks(oldShelfNo, oldQty);
                    }
                    shelfCapacityService.increaseShimPacks(item.getShelfNo(), oldQty + item.getQuantity());
                } else {
                    shelfCapacityService.increaseShimPacks(item.getShelfNo(), item.getQuantity());
                }
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

            log.info("入库登记: 型号={}, 数量={}, 货架={}, 操作人={}",
                    part.getPartModel(), item.getQuantity(), item.getShelfNo(), dto.getOperator());
        }
        return records;
    }
}
