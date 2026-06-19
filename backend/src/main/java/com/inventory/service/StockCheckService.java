package com.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.dto.StockCheckDTO;
import com.inventory.dto.StockCheckHotZoneVO;
import com.inventory.entity.SmallPart;
import com.inventory.entity.StockCheckRecord;
import com.inventory.mapper.StockCheckRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
