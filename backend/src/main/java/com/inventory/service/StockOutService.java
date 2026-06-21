package com.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.dto.StockOutDTO;
import com.inventory.entity.SmallPart;
import com.inventory.entity.StockOutRecord;
import com.inventory.exception.BusinessException;
import com.inventory.mapper.StockOutRecordMapper;
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
public class StockOutService extends ServiceImpl<StockOutRecordMapper, StockOutRecord> {

    private final StockOutRecordMapper stockOutRecordMapper;
    private final SmallPartService smallPartService;
    private final ShelfCapacityService shelfCapacityService;
    private final LineQuotaService lineQuotaService;

    public IPage<StockOutRecord> getPageList(Integer pageNum, Integer pageSize, String partModel,
                                              String productionLine, String startTime, String endTime) {
        Page<StockOutRecord> page = new Page<>(pageNum, pageSize);
        return stockOutRecordMapper.selectPageList(page, partModel, productionLine, startTime, endTime);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<StockOutRecord> stockOut(StockOutDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("出库明细不能为空");
        }
        for (StockOutDTO.StockOutItem item : dto.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("出库数量必须大于0，零件ID：" + item.getPartId());
            }
        }

        List<StockOutRecord> records = new ArrayList<>();

        Map<Long, Integer> partTotalQtyMap = new HashMap<>();
        for (StockOutDTO.StockOutItem item : dto.getItems()) {
            partTotalQtyMap.merge(item.getPartId(), item.getQuantity(), Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : partTotalQtyMap.entrySet()) {
            SmallPart part = smallPartService.getById(entry.getKey());
            if (part.getStockQuantity() < entry.getValue()) {
                throw new BusinessException("库存不足: " + part.getPartModel() +
                        ", 当前库存: " + part.getStockQuantity() +
                        ", 申请合计: " + entry.getValue());
            }
        }

        Map<Long, SmallPart> partCache = new HashMap<>();
        Map<String, Integer> partTypeQtyMap = new HashMap<>();
        for (StockOutDTO.StockOutItem item : dto.getItems()) {
            SmallPart part = partCache.computeIfAbsent(item.getPartId(), smallPartService::getById);
            partTypeQtyMap.merge(part.getPartType(), item.getQuantity(), Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : partTypeQtyMap.entrySet()) {
            lineQuotaService.consumeQuota(dto.getProductionLine(), entry.getKey(), entry.getValue());
        }

        for (StockOutDTO.StockOutItem item : dto.getItems()) {
            SmallPart part = partCache.get(item.getPartId());

            smallPartService.decreaseStock(part.getId(), item.getQuantity());

            if ("顶针".equals(part.getPartType())) {
                shelfCapacityService.decreasePinBoxes(part.getShelfNo(), item.getQuantity());
            } else if ("限位垫片".equals(part.getPartType())) {
                shelfCapacityService.decreaseShimPacks(part.getShelfNo(), item.getQuantity());
            }

            StockOutRecord record = new StockOutRecord();
            record.setPartId(part.getId());
            record.setPartModel(part.getPartModel());
            record.setQuantity(item.getQuantity());
            record.setProductionLine(dto.getProductionLine());
            record.setOperator(dto.getOperator());
            record.setReceiver(dto.getReceiver());
            record.setRemark(dto.getRemark());
            record.setCreateTime(LocalDateTime.now());
            save(record);
            records.add(record);

            log.info("出库登记: 型号={}, 数量={}, 产线={}, 货架={}, 操作人={}",
                    part.getPartModel(), item.getQuantity(), dto.getProductionLine(),
                    part.getShelfNo(), dto.getOperator());
        }
        return records;
    }
}
