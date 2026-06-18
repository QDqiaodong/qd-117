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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockOutService extends ServiceImpl<StockOutRecordMapper, StockOutRecord> {

    private final StockOutRecordMapper stockOutRecordMapper;
    private final SmallPartService smallPartService;

    public IPage<StockOutRecord> getPageList(Integer pageNum, Integer pageSize, String partModel,
                                              String productionLine, String startTime, String endTime) {
        Page<StockOutRecord> page = new Page<>(pageNum, pageSize);
        return stockOutRecordMapper.selectPageList(page, partModel, productionLine, startTime, endTime);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<StockOutRecord> stockOut(StockOutDTO dto) {
        List<StockOutRecord> records = new ArrayList<>();

        for (StockOutDTO.StockOutItem item : dto.getItems()) {
            SmallPart part = smallPartService.getById(item.getPartId());
            if (part.getStockQuantity() < item.getQuantity()) {
                throw new BusinessException("库存不足: " + part.getPartModel() +
                        ", 当前库存: " + part.getStockQuantity() +
                        ", 申请数量: " + item.getQuantity());
            }
        }

        for (StockOutDTO.StockOutItem item : dto.getItems()) {
            SmallPart part = smallPartService.getById(item.getPartId());

            smallPartService.decreaseStock(part.getId(), item.getQuantity());

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

            log.info("出库登记: 型号={}, 数量={}, 产线={}, 操作人={}",
                    part.getPartModel(), item.getQuantity(), dto.getProductionLine(), dto.getOperator());
        }
        return records;
    }
}
