package com.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.dto.DiffConfirmDTO;
import com.inventory.dto.SnapshotInitiateDTO;
import com.inventory.dto.SnapshotVO;
import com.inventory.dto.StockCheckDTO;
import com.inventory.dto.StockCheckHotZoneVO;
import com.inventory.dto.StockCheckResultVO;
import com.inventory.entity.SmallPart;
import com.inventory.entity.StockCheckRecord;
import com.inventory.entity.StockCheckSnapshot;
import com.inventory.exception.BusinessException;
import com.inventory.mapper.StockCheckRecordMapper;
import com.inventory.mapper.StockCheckSnapshotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockCheckService extends ServiceImpl<StockCheckRecordMapper, StockCheckRecord> {

    private final StockCheckRecordMapper stockCheckRecordMapper;
    private final StockCheckSnapshotMapper stockCheckSnapshotMapper;
    private final SmallPartService smallPartService;

    public IPage<StockCheckRecord> getPageList(Integer pageNum, Integer pageSize, String partModel,
                                                String quarter, String startTime, String endTime,
                                                Integer confirmStatus) {
        Page<StockCheckRecord> page = new Page<>(pageNum, pageSize);
        return stockCheckRecordMapper.selectPageList(page, partModel, quarter, startTime, endTime, confirmStatus);
    }

    public StockCheckHotZoneVO getHotZone(String quarter) {
        List<StockCheckHotZoneVO.HotZoneRow> rows = stockCheckRecordMapper.selectHotZoneRows(quarter);

        Map<String, StockCheckHotZoneVO.ShelfHotZone> shelfMap = new LinkedHashMap<>();
        int totalGain = 0;
        int totalLoss = 0;

        for (StockCheckHotZoneVO.HotZoneRow row : rows) {
            String shelfNo = (row.getShelfNo() == null || row.getShelfNo().isEmpty()) ? "未归类" : row.getShelfNo();
            StockCheckHotZoneVO.ShelfHotZone shelf = shelfMap.computeIfAbsent(shelfNo, k -> {
                StockCheckHotZoneVO.ShelfHotZone s = new StockCheckHotZoneVO.ShelfHotZone();
                s.setShelfNo(k);
                s.setPinGain(0);
                s.setPinLoss(0);
                s.setGasketGain(0);
                s.setGasketLoss(0);
                s.setTotalGain(0);
                s.setTotalLoss(0);
                s.setRowCount(0);
                s.setRows(new ArrayList<>());
                return s;
            });
            shelf.getRows().add(row);
            shelf.setRowCount(shelf.getRowCount() + 1);

            int diff = row.getDiffQuantity() == null ? 0 : row.getDiffQuantity();
            String partType = row.getPartType();
            if ("顶针".equals(partType)) {
                if (diff >= 0) {
                    shelf.setPinGain(shelf.getPinGain() + diff);
                } else {
                    shelf.setPinLoss(shelf.getPinLoss() + (-diff));
                }
            } else if ("限位垫片".equals(partType)) {
                if (diff >= 0) {
                    shelf.setGasketGain(shelf.getGasketGain() + diff);
                } else {
                    shelf.setGasketLoss(shelf.getGasketLoss() + (-diff));
                }
            }
            if (diff >= 0) {
                shelf.setTotalGain(shelf.getTotalGain() + diff);
                totalGain += diff;
            } else {
                shelf.setTotalLoss(shelf.getTotalLoss() + (-diff));
                totalLoss += (-diff);
            }
        }

        StockCheckHotZoneVO vo = new StockCheckHotZoneVO();
        vo.setShelves(new ArrayList<>(shelfMap.values()));
        vo.setTotalShelves(shelfMap.size());
        vo.setTotalGain(totalGain);
        vo.setTotalLoss(totalLoss);
        vo.setTotalRows(rows.size());
        log.info("盘点热区生成：季度={}，货架数={}，盘盈={}，盘亏={}", quarter, shelfMap.size(), totalGain, totalLoss);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public SnapshotVO initiateSnapshot(SnapshotInitiateDTO dto) {
        String quarter = dto.getQuarter();
        List<StockCheckSnapshot> existing = stockCheckSnapshotMapper.selectByQuarter(quarter);
        if (!existing.isEmpty()) {
            log.info("季度 {} 已存在 {} 条快照，重新生成快照", quarter, existing.size());
            stockCheckSnapshotMapper.deleteByQuarter(quarter);
        }

        List<SmallPart> allParts = smallPartService.listAll();
        List<StockCheckSnapshot> snapshots = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (SmallPart part : allParts) {
            StockCheckSnapshot snapshot = new StockCheckSnapshot();
            snapshot.setQuarter(quarter);
            snapshot.setPartId(part.getId());
            snapshot.setPartModel(part.getPartModel());
            snapshot.setPartName(part.getPartName());
            snapshot.setPartType(part.getPartType());
            snapshot.setFrozenStockQuantity(part.getStockQuantity());
            snapshot.setFrozenShelfNo(part.getShelfNo());
            snapshot.setCreateTime(now);
            snapshots.add(snapshot);
        }

        if (!snapshots.isEmpty()) {
            saveBatchSnapshot(snapshots);
        }

        SnapshotVO vo = new SnapshotVO();
        vo.setQuarter(quarter);
        vo.setTotalCount(snapshots.size());
        vo.setPinCount((int) snapshots.stream().filter(s -> "顶针".equals(s.getPartType())).count());
        vo.setShimCount((int) snapshots.stream().filter(s -> "限位垫片".equals(s.getPartType())).count());
        vo.setItems(snapshots);
        log.info("季度盘点快照创建成功：季度={}，共 {} 条记录（顶针{}，垫片{}）",
                quarter, vo.getTotalCount(), vo.getPinCount(), vo.getShimCount());
        return vo;
    }

    private void saveBatchSnapshot(List<StockCheckSnapshot> snapshots) {
        for (StockCheckSnapshot snapshot : snapshots) {
            stockCheckSnapshotMapper.insert(snapshot);
        }
    }

    public SnapshotVO getSnapshot(String quarter) {
        List<StockCheckSnapshot> snapshots = stockCheckSnapshotMapper.selectByQuarter(quarter);
        SnapshotVO vo = new SnapshotVO();
        vo.setQuarter(quarter);
        vo.setTotalCount(snapshots.size());
        vo.setPinCount((int) snapshots.stream().filter(s -> "顶针".equals(s.getPartType())).count());
        vo.setShimCount((int) snapshots.stream().filter(s -> "限位垫片".equals(s.getPartType())).count());
        vo.setItems(snapshots);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public StockCheckResultVO checkStock(StockCheckDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("盘点明细不能为空");
        }
        for (StockCheckDTO.StockCheckItem item : dto.getItems()) {
            if (item.getActualQuantity() == null || item.getActualQuantity() < 0) {
                throw new BusinessException("实际库存数量不能为负数，零件ID：" + item.getPartId());
            }
        }

        StockCheckResultVO result = new StockCheckResultVO();
        List<StockCheckRecord> addedRecords = new ArrayList<>();
        List<StockCheckRecord> duplicateRecords = new ArrayList<>();

        List<Long> partIds = dto.getItems().stream()
                .map(StockCheckDTO.StockCheckItem::getPartId)
                .collect(Collectors.toList());

        Map<Long, StockCheckSnapshot> snapshotMap = new LinkedHashMap<>();
        for (Long partId : partIds) {
            StockCheckSnapshot snapshot = stockCheckSnapshotMapper.selectByQuarterAndPartId(dto.getQuarter(), partId);
            if (snapshot != null) {
                snapshotMap.put(partId, snapshot);
            }
        }

        if (snapshotMap.isEmpty()) {
            throw new BusinessException("该季度尚未创建盘点快照，请先发起盘点初始化");
        }

        List<String> partModels = snapshotMap.values().stream()
                .map(StockCheckSnapshot::getPartModel)
                .collect(Collectors.toList());

        List<StockCheckRecord> existingRecords = stockCheckRecordMapper
                .selectByQuarterAndPartModels(dto.getQuarter(), partModels);

        Set<String> existingModelSet = existingRecords.stream()
                .map(StockCheckRecord::getPartModel)
                .collect(Collectors.toSet());

        Map<String, StockCheckRecord> existingRecordMap = existingRecords.stream()
                .collect(Collectors.toMap(StockCheckRecord::getPartModel, r -> r));

        for (StockCheckDTO.StockCheckItem item : dto.getItems()) {
            StockCheckSnapshot snapshot = snapshotMap.get(item.getPartId());
            if (snapshot == null) {
                continue;
            }

            if (existingModelSet.contains(snapshot.getPartModel())) {
                duplicateRecords.add(existingRecordMap.get(snapshot.getPartModel()));
                continue;
            }

            int systemQuantity = snapshot.getFrozenStockQuantity();
            int actualQuantity = item.getActualQuantity();
            int diffQuantity = actualQuantity - systemQuantity;

            StockCheckRecord record = new StockCheckRecord();
            record.setPartId(snapshot.getPartId());
            record.setPartModel(snapshot.getPartModel());
            record.setSystemQuantity(systemQuantity);
            record.setActualQuantity(actualQuantity);
            record.setDiffQuantity(diffQuantity);
            record.setShelfNo(snapshot.getFrozenShelfNo());
            record.setCheckPerson(dto.getCheckPerson());
            record.setRemark(item.getRemark());
            record.setQuarter(dto.getQuarter());
            record.setSnapshotId(snapshot.getId());
            record.setConfirmStatus(diffQuantity == 0 ? 1 : 0);
            record.setCreateTime(LocalDateTime.now());
            save(record);
            addedRecords.add(record);

            if (diffQuantity != 0) {
                log.warn("库存差异(基于快照): 型号={}, 快照库存={}, 实际库存={}, 差异={}",
                        snapshot.getPartModel(), systemQuantity, actualQuantity, diffQuantity);
            } else {
                log.info("盘点一致(基于快照): 型号={}, 库存数量={}", snapshot.getPartModel(), actualQuantity);
            }
        }

        result.setAddedRecords(addedRecords);
        result.setDuplicateRecords(duplicateRecords);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmDiff(DiffConfirmDTO dto) {
        StockCheckRecord record = getById(dto.getRecordId());
        if (record == null) {
            throw new BusinessException("盘点记录不存在");
        }
        if (record.getDiffQuantity() == null || record.getDiffQuantity() == 0) {
            throw new BusinessException("该记录无差异，无需确认");
        }
        if (record.getConfirmStatus() != null && record.getConfirmStatus() == 1) {
            throw new BusinessException("该差异已确认闭环，不可重复操作");
        }

        int rows = stockCheckRecordMapper.updateConfirm(
                dto.getRecordId(),
                dto.getHandleConclusion(),
                dto.getConfirmPerson(),
                LocalDateTime.now()
        );
        if (rows != 1) {
            throw new BusinessException("差异确认失败");
        }
        log.info("盘点差异确认成功：记录ID={}, 型号={}, 差异={}, 确认人={}, 处理结论={}",
                record.getId(), record.getPartModel(), record.getDiffQuantity(),
                dto.getConfirmPerson(), dto.getHandleConclusion());
    }
}
