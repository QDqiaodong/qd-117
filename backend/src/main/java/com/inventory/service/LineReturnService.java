package com.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.dto.LineReturnDTO;
import com.inventory.entity.LineReturnRecord;
import com.inventory.entity.SmallPart;
import com.inventory.exception.BusinessException;
import com.inventory.mapper.LineReturnRecordMapper;
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
public class LineReturnService extends ServiceImpl<LineReturnRecordMapper, LineReturnRecord> {

    private final LineReturnRecordMapper lineReturnRecordMapper;
    private final SmallPartService smallPartService;
    private final ShelfCapacityService shelfCapacityService;
    private final LineQuotaService lineQuotaService;

    public IPage<LineReturnRecord> getPageList(Integer pageNum, Integer pageSize, String partModel,
                                                String productionLine, String reusableStatus,
                                                String startTime, String endTime) {
        Page<LineReturnRecord> page = new Page<>(pageNum, pageSize);
        return lineReturnRecordMapper.selectPageList(page, partModel, productionLine, reusableStatus, startTime, endTime);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<LineReturnRecord> lineReturn(LineReturnDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("退回明细不能为空");
        }
        if (dto.getOperator() != null) {
            dto.setOperator(dto.getOperator().trim());
        }
        if (dto.getOriginalReceiver() != null) {
            dto.setOriginalReceiver(dto.getOriginalReceiver().trim());
        }
        if (dto.getRemark() != null) {
            dto.setRemark(dto.getRemark().trim());
        }

        for (LineReturnDTO.LineReturnItem item : dto.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("退回数量必须大于0，零件ID：" + item.getPartId());
            }
            if (item.getQualifiedQuantity() == null || item.getQualifiedQuantity() < 0) {
                throw new BusinessException("合格数量不能为负数，零件ID：" + item.getPartId());
            }
            if (item.getUnqualifiedQuantity() == null || item.getUnqualifiedQuantity() < 0) {
                throw new BusinessException("不合格数量不能为负数，零件ID：" + item.getPartId());
            }
            int total = item.getQualifiedQuantity() + item.getUnqualifiedQuantity();
            if (total != item.getQuantity()) {
                throw new BusinessException("合格数量与不合格数量之和必须等于退回数量，零件ID：" + item.getPartId());
            }
            if (item.getReusableStatus() != null) {
                item.setReusableStatus(item.getReusableStatus().trim());
                String status = item.getReusableStatus();
                if ("全部合格".equals(status) && item.getUnqualifiedQuantity() > 0) {
                    throw new BusinessException("可复用状态为「全部合格」时，不合格数量必须为0，零件ID：" + item.getPartId());
                }
                if ("全部不合格".equals(status) && item.getQualifiedQuantity() > 0) {
                    throw new BusinessException("可复用状态为「全部不合格」时，合格数量必须为0，零件ID：" + item.getPartId());
                }
                if ("部分合格".equals(status) && (item.getQualifiedQuantity() <= 0 || item.getUnqualifiedQuantity() <= 0)) {
                    throw new BusinessException("可复用状态为「部分合格」时，合格和不合格数量都应大于0，零件ID：" + item.getPartId());
                }
            }
        }

        List<LineReturnRecord> records = new ArrayList<>();
        Map<Long, SmallPart> partCache = new HashMap<>();

        Map<String, Integer> partTypeQualifiedQtyMap = new HashMap<>();
        for (LineReturnDTO.LineReturnItem item : dto.getItems()) {
            SmallPart part = partCache.computeIfAbsent(item.getPartId(), smallPartService::getById);
            if (item.getQualifiedQuantity() > 0) {
                partTypeQualifiedQtyMap.merge(part.getPartType(), item.getQualifiedQuantity(), Integer::sum);
            }
        }

        for (Map.Entry<String, Integer> entry : partTypeQualifiedQtyMap.entrySet()) {
            lineQuotaService.returnQuota(dto.getProductionLine(), entry.getKey(), entry.getValue());
        }

        for (LineReturnDTO.LineReturnItem item : dto.getItems()) {
            SmallPart part = partCache.get(item.getPartId());

            if (item.getQualifiedQuantity() > 0) {
                smallPartService.increaseStock(part.getId(), item.getQualifiedQuantity());

                if ("顶针".equals(part.getPartType())) {
                    shelfCapacityService.increasePinBoxes(part.getShelfNo(), item.getQualifiedQuantity());
                } else if ("限位垫片".equals(part.getPartType())) {
                    shelfCapacityService.increaseShimPacks(part.getShelfNo(), item.getQualifiedQuantity());
                }
            }

            LineReturnRecord record = new LineReturnRecord();
            record.setPartId(part.getId());
            record.setPartModel(part.getPartModel());
            record.setQuantity(item.getQuantity());
            record.setQualifiedQuantity(item.getQualifiedQuantity());
            record.setUnqualifiedQuantity(item.getUnqualifiedQuantity());
            record.setReusableStatus(item.getReusableStatus());
            record.setProductionLine(dto.getProductionLine());
            record.setOriginalReceiver(dto.getOriginalReceiver());
            record.setOperator(dto.getOperator());
            record.setRemark(dto.getRemark());
            record.setCreateTime(LocalDateTime.now());
            save(record);
            records.add(record);

            log.info("产线退回登记: 型号={}, 退回总数={}, 合格={}, 不合格={}, 状态={}, 产线={}, 原领用人={}, 操作人={}",
                    part.getPartModel(), item.getQuantity(), item.getQualifiedQuantity(),
                    item.getUnqualifiedQuantity(), item.getReusableStatus(),
                    dto.getProductionLine(), dto.getOriginalReceiver(), dto.getOperator());
        }
        return records;
    }
}
