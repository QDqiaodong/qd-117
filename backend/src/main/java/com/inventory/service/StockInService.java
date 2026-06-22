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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockInService extends ServiceImpl<StockInRecordMapper, StockInRecord> {

    private final StockInRecordMapper stockInRecordMapper;
    private final SmallPartService smallPartService;
    private final ShelfCapacityService shelfCapacityService;
    private final PinBoxService pinBoxService;

    public IPage<StockInRecord> getPageList(Integer pageNum, Integer pageSize, String partModel, String startTime, String endTime) {
        Page<StockInRecord> page = new Page<>(pageNum, pageSize);
        return stockInRecordMapper.selectPageList(page, partModel, startTime, endTime);
    }

    public StockInValidationVO validate(StockInDTO dto) {
        return shelfCapacityService.validateStockIn(dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<StockInRecord> stockIn(StockInDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("入库明细不能为空");
        }
        for (StockInDTO.StockInItem item : dto.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("入库数量必须大于0，型号：" + item.getPartModel());
            }
        }

        for (StockInDTO.StockInItem item : dto.getItems()) {
            trimItem(item);
        }

        checkStockInBoxNoQuantityConsistency(dto.getItems());
        checkStockInBoxNoConflicts(dto.getItems());
        checkStockInConflicts(dto.getItems());

        StockInValidationVO validation = validate(dto);
        if (!validation.isValid()) {
            String errorMsg = String.join("; ", validation.getErrors());
            throw new BusinessException(errorMsg);
        }

        List<StockInRecord> records = new ArrayList<>();
        for (StockInDTO.StockInItem item : dto.getItems()) {
            SmallPart existingPart = smallPartService.getByModel(item.getPartModel());
            String oldShelfNo = existingPart != null ? existingPart.getShelfNo() : null;
            String oldPartTypeForUpdate = existingPart != null ? existingPart.getPartType() : item.getPartType();
            boolean isNewPart = existingPart == null;
            boolean shelfChanged = existingPart != null && item.getShelfNo() != null
                    && !item.getShelfNo().isEmpty() && !item.getShelfNo().equals(existingPart.getShelfNo());
            int oldQty = existingPart != null && existingPart.getStockQuantity() != null
                    ? existingPart.getStockQuantity() : 0;

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
                if (item.getPartType() != null && !item.getPartType().isEmpty()) {
                    part.setPartType(item.getPartType());
                }
                if (item.getSpecParams() != null && !item.getSpecParams().isEmpty()) {
                    part.setSpecParams(item.getSpecParams());
                }
                if (item.getShelfNo() != null && !item.getShelfNo().isEmpty()) {
                    part.setShelfNo(item.getShelfNo());
                }
                if (item.getUnit() != null && !item.getUnit().isEmpty()) {
                    part.setUnit(item.getUnit());
                }
                if (item.getRemark() != null && !item.getRemark().isEmpty()) {
                    part.setRemark(item.getRemark());
                }
                boolean partTypeChangedForUpdate = !oldPartTypeForUpdate.equals(part.getPartType());
                smallPartService.updateAndIncreaseStock(part, partTypeChangedForUpdate, oldPartTypeForUpdate, item.getQuantity());
            }

            String newPartType = item.getPartType();
            String oldPartType = existingPart != null ? existingPart.getPartType() : newPartType;
            boolean partTypeChanged = !oldPartType.equals(newPartType);

            if (partTypeChanged && existingPart != null) {
                if ("顶针".equals(oldPartType) && oldQty > 0) {
                    shelfCapacityService.decreasePinBoxes(existingPart.getShelfNo(), oldQty);
                } else if ("限位垫片".equals(oldPartType) && oldQty > 0) {
                    shelfCapacityService.decreaseShimPacks(existingPart.getShelfNo(), oldQty);
                }
                if ("顶针".equals(newPartType)) {
                    shelfCapacityService.increasePinBoxes(item.getShelfNo(), oldQty + item.getQuantity());
                } else if ("限位垫片".equals(newPartType)) {
                    shelfCapacityService.increaseShimPacks(item.getShelfNo(), oldQty + item.getQuantity());
                }
            } else if ("顶针".equals(newPartType)) {
                if (shelfChanged && oldShelfNo != null) {
                    if (oldQty > 0) {
                        shelfCapacityService.decreasePinBoxes(oldShelfNo, oldQty);
                    }
                    shelfCapacityService.increasePinBoxes(item.getShelfNo(), oldQty + item.getQuantity());
                } else {
                    shelfCapacityService.increasePinBoxes(item.getShelfNo(), item.getQuantity());
                }
            } else if ("限位垫片".equals(newPartType)) {
                if (shelfChanged && oldShelfNo != null) {
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

            if ("顶针".equals(item.getPartType()) && item.getBoxNoStart() != null && !item.getBoxNoStart().trim().isEmpty()) {
                String boxNoStart = item.getBoxNoStart().trim();
                String boxNoEnd = (item.getBoxNoEnd() != null && !item.getBoxNoEnd().trim().isEmpty()) ? item.getBoxNoEnd().trim() : boxNoStart;
                List<com.inventory.entity.PinBox> boxes = pinBoxService.createBoxesFromRange(
                        part.getId(), part.getPartModel(), item.getShelfNo(),
                        boxNoStart, boxNoEnd, record.getId(), item.getRemark()
                );
                String boxNosStr = boxes.stream().map(com.inventory.entity.PinBox::getBoxNo)
                        .collect(java.util.stream.Collectors.joining(","));
                record.setBoxNos(boxNosStr);
                updateById(record);
            }

            log.info("入库登记: 型号={}, 数量={}, 货架={}, 操作人={}",
                    part.getPartModel(), item.getQuantity(), item.getShelfNo(), dto.getOperator());
        }
        return records;
    }

    private void trimItem(StockInDTO.StockInItem item) {
        if (item.getPartModel() != null) {
            item.setPartModel(item.getPartModel().trim());
        }
        if (item.getPartName() != null) {
            item.setPartName(item.getPartName().trim());
        }
        if (item.getPartType() != null) {
            item.setPartType(item.getPartType().trim());
        }
        if (item.getShelfNo() != null) {
            item.setShelfNo(item.getShelfNo().trim());
        }
        if (item.getUnit() != null) {
            item.setUnit(item.getUnit().trim());
        }
        if (item.getRemark() != null) {
            item.setRemark(item.getRemark().trim());
        }
        if (item.getSpecParams() != null) {
            item.setSpecParams(item.getSpecParams().trim());
        }
        if (item.getBoxNoStart() != null) {
            item.setBoxNoStart(item.getBoxNoStart().trim());
        }
        if (item.getBoxNoEnd() != null) {
            item.setBoxNoEnd(item.getBoxNoEnd().trim());
        }
    }

    private void checkStockInConflicts(List<StockInDTO.StockInItem> items) {
        List<String> conflicts = new ArrayList<>();
        Map<String, StockInDTO.StockInItem> batchFirstSeen = new HashMap<>();
        for (StockInDTO.StockInItem item : items) {
            String model = item.getPartModel();
            SmallPart existing = smallPartService.getByModel(model);
            String refName;
            String refType;
            String refShelf;
            if (existing != null) {
                refName = existing.getPartName();
                refType = existing.getPartType();
                refShelf = existing.getShelfNo();
            } else if (batchFirstSeen.containsKey(model)) {
                StockInDTO.StockInItem first = batchFirstSeen.get(model);
                refName = first.getPartName();
                refType = first.getPartType();
                refShelf = first.getShelfNo();
            } else {
                batchFirstSeen.put(model, item);
                continue;
            }
            List<String> diffs = new ArrayList<>();
            String name = item.getPartName();
            String type = item.getPartType();
            String shelf = item.getShelfNo();
            if (name != null && !name.isEmpty() && refName != null && !refName.isEmpty() && !name.equals(refName)) {
                diffs.add("名称不一致(库存:" + refName + " ≠ 本次:" + name + ")");
            }
            if (type != null && !type.isEmpty() && refType != null && !refType.isEmpty() && !type.equals(refType)) {
                diffs.add("类型不一致(库存:" + refType + " ≠ 本次:" + type + ")");
            }
            if (shelf != null && !shelf.isEmpty() && refShelf != null && !refShelf.isEmpty() && !shelf.equals(refShelf)) {
                diffs.add("货架不一致(库存:" + refShelf + " ≠ 本次:" + shelf + ")");
            }
            if (!diffs.isEmpty()) {
                conflicts.add("型号[" + model + "] " + String.join("，", diffs));
            }
        }
        if (!conflicts.isEmpty()) {
            throw new BusinessException("入库冲突：同型号但名称/类型/货架不一致，已拒绝累加库存，请核对后重新录入。"
                    + String.join("；", conflicts));
        }
    }

    private List<String> parseBoxRange(String start, String end) {
        List<String> result = new ArrayList<>();
        start = start != null ? start.trim() : "";
        end = end != null ? end.trim() : "";
        if (start.isEmpty() || end.isEmpty()) {
            return result;
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(.*?)(\\d+)$");
        java.util.regex.Matcher startMatcher = pattern.matcher(start);
        java.util.regex.Matcher endMatcher = pattern.matcher(end);
        if (!startMatcher.matches() || !endMatcher.matches()) {
            result.add(start);
            if (!start.equals(end)) {
                result.add(end);
            }
            return result;
        }
        String prefix = startMatcher.group(1);
        String endPrefix = endMatcher.group(1);
        if (!prefix.equals(endPrefix)) {
            result.add(start);
            if (!start.equals(end)) {
                result.add(end);
            }
            return result;
        }
        int startNum = Integer.parseInt(startMatcher.group(2));
        int endNum = Integer.parseInt(endMatcher.group(2));
        int numWidth = startMatcher.group(2).length();
        if (startNum > endNum) {
            int tmp = startNum;
            startNum = endNum;
            endNum = tmp;
        }
        for (int i = startNum; i <= endNum; i++) {
            result.add(prefix + String.format("%0" + numWidth + "d", i));
        }
        return result;
    }

    private void checkStockInBoxNoQuantityConsistency(List<StockInDTO.StockInItem> items) {
        for (int i = 0; i < items.size(); i++) {
            StockInDTO.StockInItem item = items.get(i);
            int rowNum = i + 1;
            if (!"顶针".equals(item.getPartType())) {
                continue;
            }
            if (item.getBoxNoStart() == null || item.getBoxNoStart().trim().isEmpty()) {
                continue;
            }
            String start = item.getBoxNoStart().trim();
            String end = (item.getBoxNoEnd() != null && !item.getBoxNoEnd().trim().isEmpty())
                    ? item.getBoxNoEnd().trim() : start;
            List<String> boxNos = parseBoxRange(start, end);
            if (!boxNos.isEmpty() && boxNos.size() != item.getQuantity()) {
                throw new BusinessException("第" + rowNum + "行盒号范围生成" + boxNos.size()
                        + "个盒号，与入库数量" + item.getQuantity() + "不一致");
            }
        }
    }

    private void checkStockInBoxNoConflicts(List<StockInDTO.StockInItem> items) {
        Map<String, List<Integer>> boxNoRowMap = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            StockInDTO.StockInItem item = items.get(i);
            int rowNum = i + 1;
            if (!"顶针".equals(item.getPartType())) {
                continue;
            }
            if (item.getBoxNoStart() == null || item.getBoxNoStart().trim().isEmpty()) {
                continue;
            }
            String start = item.getBoxNoStart().trim();
            String end = (item.getBoxNoEnd() != null && !item.getBoxNoEnd().trim().isEmpty())
                    ? item.getBoxNoEnd().trim() : start;
            List<String> boxNos = parseBoxRange(start, end);
            for (String boxNo : boxNos) {
                boxNoRowMap.computeIfAbsent(boxNo, k -> new ArrayList<>()).add(rowNum);
            }
        }
        List<String> conflicts = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : boxNoRowMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                String rows = entry.getValue().stream()
                        .map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining("、"));
                conflicts.add("盒号[" + entry.getKey() + "]重复出现在第" + rows + "行");
            }
        }
        if (!conflicts.isEmpty()) {
            throw new BusinessException("入库冲突：同一批次中盒号范围不能重叠，" + String.join("；", conflicts));
        }
    }
}
