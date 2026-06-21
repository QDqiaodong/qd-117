package com.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.entity.PinBox;
import com.inventory.exception.BusinessException;
import com.inventory.mapper.PinBoxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PinBoxService extends ServiceImpl<PinBoxMapper, PinBox> {

    private final PinBoxMapper pinBoxMapper;

    public IPage<PinBox> getPageList(Integer pageNum, Integer pageSize, String partModel,
                                      String status, String boxNo, String productionLine,
                                      String startTime, String endTime) {
        Page<PinBox> page = new Page<>(pageNum, pageSize);
        return pinBoxMapper.selectPageList(page, partModel, status, boxNo, productionLine, startTime, endTime);
    }

    public List<PinBox> getAvailableByPartId(Long partId) {
        return pinBoxMapper.selectAvailableByPartId(partId);
    }

    public List<PinBox> getByStockInRecordId(Long stockInRecordId) {
        return pinBoxMapper.selectByStockInRecordId(stockInRecordId);
    }

    public List<PinBox> getByStockOutRecordId(Long stockOutRecordId) {
        return pinBoxMapper.selectByStockOutRecordId(stockOutRecordId);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<PinBox> createBoxesFromRange(Long partId, String partModel, String shelfNo,
                                              String boxNoStart, String boxNoEnd,
                                              Long stockInRecordId, String remark) {
        List<String> boxNos = parseBoxRange(boxNoStart, boxNoEnd);
        if (boxNos.isEmpty()) {
            throw new BusinessException("盒号范围解析失败，请检查格式");
        }

        List<PinBox> existing = pinBoxMapper.selectByBoxNos(boxNos);
        if (!existing.isEmpty()) {
            Set<String> existingNos = new HashSet<>();
            for (PinBox box : existing) {
                existingNos.add(box.getBoxNo());
            }
            throw new BusinessException("以下盒号已存在：" + String.join("、", existingNos));
        }

        LocalDateTime now = LocalDateTime.now();
        List<PinBox> created = new ArrayList<>();
        for (String boxNo : boxNos) {
            PinBox box = new PinBox();
            box.setBoxNo(boxNo);
            box.setPartId(partId);
            box.setPartModel(partModel);
            box.setStatus("IN_STOCK");
            box.setStockInRecordId(stockInRecordId);
            box.setShelfNo(shelfNo);
            box.setRemark(remark);
            box.setCreateTime(now);
            box.setUpdateTime(now);
            save(box);
            created.add(box);
        }

        log.info("创建顶针盒号成功: 型号={}, 盒号数量={}, 范围={}-{}", partModel, created.size(), boxNoStart, boxNoEnd);
        return created;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<PinBox> consumeBoxes(List<String> boxNos, Long partId, Long stockOutRecordId,
                                      String productionLine, String remark) {
        if (boxNos == null || boxNos.isEmpty()) {
            throw new BusinessException("请选择要出库的盒号");
        }

        List<PinBox> boxes = pinBoxMapper.selectByBoxNos(boxNos);
        if (boxes.size() != boxNos.size()) {
            Set<String> found = new HashSet<>();
            for (PinBox box : boxes) {
                found.add(box.getBoxNo());
            }
            List<String> missing = new ArrayList<>();
            for (String no : boxNos) {
                if (!found.contains(no)) {
                    missing.add(no);
                }
            }
            throw new BusinessException("以下盒号不存在：" + String.join("、", missing));
        }

        for (PinBox box : boxes) {
            if (!partId.equals(box.getPartId())) {
                throw new BusinessException("盒号[" + box.getBoxNo() + "]不属于当前选择的零件型号");
            }
            if (!"IN_STOCK".equals(box.getStatus())) {
                throw new BusinessException("盒号[" + box.getBoxNo() + "]当前状态不是在库，无法出库");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (PinBox box : boxes) {
            box.setStatus("OUT_OF_STOCK");
            box.setStockOutRecordId(stockOutRecordId);
            box.setProductionLine(productionLine);
            if (remark != null && !remark.isEmpty()) {
                box.setRemark(remark);
            }
            box.setUpdateTime(now);
            updateById(box);
        }

        log.info("顶针盒号出库成功: 数量={}, 产线={}", boxes.size(), productionLine);
        return boxes;
    }

    private List<String> parseBoxRange(String start, String end) {
        List<String> result = new ArrayList<>();

        start = start != null ? start.trim() : "";
        end = end != null ? end.trim() : "";

        if (start.isEmpty() || end.isEmpty()) {
            return result;
        }

        Pattern pattern = Pattern.compile("^(.*?)(\\d+)$");
        Matcher startMatcher = pattern.matcher(start);
        Matcher endMatcher = pattern.matcher(end);

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
}
