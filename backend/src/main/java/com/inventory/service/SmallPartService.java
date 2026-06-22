package com.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.cache.PartSpecCache;
import com.inventory.common.WarningLevel;
import com.inventory.dto.PartSpecCacheDiagnosisVO;
import com.inventory.dto.PinMatrixVO;
import com.inventory.dto.ShimMatrixVO;
import com.inventory.entity.SmallPart;
import com.inventory.exception.BusinessException;
import com.inventory.mapper.SmallPartMapper;
import com.inventory.spec.ParsedSpec;
import com.inventory.spec.SpecParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmallPartService extends ServiceImpl<SmallPartMapper, SmallPart> {

    private final SmallPartMapper smallPartMapper;
    private final PartSpecCache partSpecCache;
    private final SpecParser specParser;
    private final StockThresholdConfigService stockThresholdConfigService;

    public IPage<SmallPart> getPageList(Integer pageNum, Integer pageSize, String partType, String keyword) {
        Page<SmallPart> page = new Page<>(pageNum, pageSize);
        IPage<SmallPart> result = smallPartMapper.selectPageList(page, partType, keyword);
        for (SmallPart part : result.getRecords()) {
            setWarningLevel(part);
        }
        return result;
    }

    public SmallPart getById(Long id) {
        SmallPart part = super.getById(id);
        if (part == null) {
            throw new BusinessException("小件不存在");
        }
        setWarningLevel(part);
        return part;
    }

    public SmallPart getByModel(String partModel) {
        if (partModel == null) {
            return null;
        }
        String trimmedModel = partModel.trim();
        return lambdaQuery().eq(SmallPart::getPartModel, trimmedModel).one();
    }

    @Transactional(rollbackFor = Exception.class)
    public SmallPart create(SmallPart part) {
        if (part.getPartModel() != null) {
            part.setPartModel(part.getPartModel().trim());
        }
        if (part.getPartName() != null) {
            part.setPartName(part.getPartName().trim());
        }
        if (part.getShelfNo() != null) {
            part.setShelfNo(part.getShelfNo().trim());
        }
        if (part.getUnit() != null) {
            part.setUnit(part.getUnit().trim());
        }
        SmallPart existing = getByModel(part.getPartModel());
        if (existing != null) {
            throw new BusinessException("零件型号已存在: " + part.getPartModel());
        }
        if (part.getStockQuantity() == null) {
            part.setStockQuantity(0);
        }
        if (part.getUnit() == null || part.getUnit().isEmpty()) {
            part.setUnit("件");
        }
        part.setCreateTime(LocalDateTime.now());
        part.setUpdateTime(LocalDateTime.now());
        save(part);
        partSpecCache.addPartSpec(part);
        log.info("创建小件: {}", part.getPartModel());
        return part;
    }

    private void trimPartFields(SmallPart part) {
        if (part.getPartModel() != null) {
            part.setPartModel(part.getPartModel().trim());
        }
        if (part.getPartName() != null) {
            part.setPartName(part.getPartName().trim());
        }
        if (part.getShelfNo() != null) {
            part.setShelfNo(part.getShelfNo().trim());
        }
        if (part.getUnit() != null) {
            part.setUnit(part.getUnit().trim());
        }
        if (part.getRemark() != null) {
            part.setRemark(part.getRemark().trim());
        }
        if (part.getSpecParams() != null) {
            part.setSpecParams(part.getSpecParams().trim());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public SmallPart update(SmallPart part) {
        return update(part, false, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public SmallPart update(SmallPart part, boolean partTypeChanged, String oldPartType) {
        SmallPart oldPart = getById(part.getId());
        trimPartFields(part);
        part.setUpdateTime(LocalDateTime.now());
        updateById(part);
        SmallPart updated = getById(part.getId());
        if (partTypeChanged && oldPartType != null) {
            partSpecCache.removePartSpecByType(oldPart, oldPartType);
            partSpecCache.addPartSpec(updated);
        } else {
            partSpecCache.updatePartSpec(oldPart, updated);
        }
        log.info("更新小件: {}", part.getPartModel());
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SmallPart part = getById(id);
        if (part.getStockQuantity() > 0) {
            throw new BusinessException("库存不为零，无法删除");
        }
        removeById(id);
        partSpecCache.removePartSpec(part);
        log.info("删除小件: {}", part.getPartModel());
    }

    @Transactional(rollbackFor = Exception.class)
    public SmallPart updateAndIncreaseStock(SmallPart part, boolean partTypeChanged, String oldPartType, int quantity) {
        SmallPart originalPart = getById(part.getId());
        trimPartFields(part);
        part.setUpdateTime(LocalDateTime.now());
        updateById(part);
        int rows = smallPartMapper.increaseStockAtomic(part.getId(), quantity);
        if (rows != 1) {
            throw new BusinessException("库存增加失败: " + originalPart.getPartModel());
        }
        SmallPart finalPart = getById(part.getId());
        if (partTypeChanged && oldPartType != null) {
            partSpecCache.removePartSpecByType(originalPart, oldPartType);
            partSpecCache.addPartSpec(finalPart);
        } else {
            partSpecCache.updatePartSpec(originalPart, finalPart);
        }
        log.info("更新并增加库存: {}, +{}", finalPart.getPartModel(), quantity);
        return finalPart;
    }

    @Transactional(rollbackFor = Exception.class)
    public void increaseStock(Long id, Integer quantity) {
        SmallPart before = getById(id);
        int rows = smallPartMapper.increaseStockAtomic(id, quantity);
        if (rows != 1) {
            throw new BusinessException("库存增加失败: " + before.getPartModel());
        }
        SmallPart after = getById(id);
        partSpecCache.updatePartSpec(before, after);
    }

    @Transactional(rollbackFor = Exception.class)
    public void decreaseStock(Long id, Integer quantity) {
        SmallPart before = getById(id);
        int rows = smallPartMapper.decreaseStockAtomic(id, quantity);
        if (rows != 1) {
            SmallPart current = getById(id);
            throw new BusinessException("库存不足: " + current.getPartModel() +
                    ", 当前库存: " + current.getStockQuantity() +
                    ", 扣减数量: " + quantity);
        }
        SmallPart after = getById(id);
        partSpecCache.updatePartSpec(before, after);
    }

    public List<SmallPart> listAll() {
        List<SmallPart> list = list();
        for (SmallPart part : list) {
            setWarningLevel(part);
        }
        return list;
    }

    private void setWarningLevel(SmallPart part) {
        WarningLevel level = stockThresholdConfigService.calculateWarningLevel(
                part.getPartType(), part.getStockQuantity());
        part.setWarningLevel(level.getCode());
    }

    public void refreshCache() {
        partSpecCache.clearCache();
        List<SmallPart> parts = list();
        for (SmallPart part : parts) {
            partSpecCache.addPartSpec(part);
        }
        log.info("小件规格缓存刷新完成，共 {} 条记录", parts.size());
    }

    public PartSpecCacheDiagnosisVO diagnoseCache() {
        PartSpecCacheDiagnosisVO vo = new PartSpecCacheDiagnosisVO();

        List<SmallPart> allDbParts = list();
        List<SmallPart> dbPins = allDbParts.stream()
                .filter(p -> "顶针".equals(p.getPartType()))
                .collect(Collectors.toList());
        List<SmallPart> dbGaskets = allDbParts.stream()
                .filter(p -> "限位垫片".equals(p.getPartType()))
                .collect(Collectors.toList());

        vo.setTotalDbCount(allDbParts.size());
        vo.setPinDbCount(dbPins.size());
        vo.setGasketDbCount(dbGaskets.size());

        vo.setTotalCacheCount((int) partSpecCache.getTotalSpecCount());
        vo.setPinCacheCount((int) partSpecCache.getSpecCountByType("顶针"));
        vo.setGasketCacheCount((int) partSpecCache.getSpecCountByType("限位垫片"));

        vo.setCacheLastUpdateTime(partSpecCache.getLastUpdateTime());
        LocalDateTime dbLastUpdate = allDbParts.stream()
                .map(SmallPart::getUpdateTime)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        vo.setDbLastUpdateTime(dbLastUpdate);

        List<PartSpecCache.PartSpecInfo> allCacheSpecs = partSpecCache.getAllSpecs();
        Set<String> cacheModels = allCacheSpecs.stream()
                .map(PartSpecCache.PartSpecInfo::getPartModel)
                .collect(Collectors.toSet());
        Set<String> dbModels = allDbParts.stream()
                .map(SmallPart::getPartModel)
                .collect(Collectors.toSet());

        List<String> missingInCache = dbModels.stream()
                .filter(model -> !cacheModels.contains(model))
                .sorted()
                .collect(Collectors.toList());
        vo.setMissingInCache(missingInCache);

        List<String> missingInDb = cacheModels.stream()
                .filter(model -> !dbModels.contains(model))
                .sorted()
                .collect(Collectors.toList());
        vo.setMissingInDb(missingInDb);

        Map<String, SmallPart> dbPartMap = new HashMap<>();
        for (SmallPart part : allDbParts) {
            dbPartMap.put(part.getPartModel(), part);
        }
        Map<String, PartSpecCache.PartSpecInfo> cacheSpecMap = new HashMap<>();
        for (PartSpecCache.PartSpecInfo info : allCacheSpecs) {
            cacheSpecMap.put(info.getPartModel(), info);
        }

        List<PartSpecCacheDiagnosisVO.SpecDiff> diffs = new ArrayList<>();
        for (String model : dbModels) {
            if (!cacheModels.contains(model)) {
                continue;
            }
            SmallPart dbPart = dbPartMap.get(model);
            PartSpecCache.PartSpecInfo cacheInfo = cacheSpecMap.get(model);
            boolean hasDiff = false;
            if (!Objects.equals(dbPart.getSpecParams(), cacheInfo.getSpecParams())) {
                hasDiff = true;
            }
            if (!Objects.equals(dbPart.getStockQuantity(), cacheInfo.getStockQuantity())) {
                hasDiff = true;
            }
            if (!Objects.equals(dbPart.getShelfNo(), cacheInfo.getShelfNo())) {
                hasDiff = true;
            }
            if (hasDiff) {
                PartSpecCacheDiagnosisVO.SpecDiff diff = new PartSpecCacheDiagnosisVO.SpecDiff();
                diff.setPartModel(model);
                diff.setPartType(dbPart.getPartType());
                diff.setDbSpecParams(dbPart.getSpecParams());
                diff.setCacheSpecParams(cacheInfo.getSpecParams());
                diff.setDbStockQuantity(dbPart.getStockQuantity());
                diff.setCacheStockQuantity(cacheInfo.getStockQuantity());
                diff.setDbShelfNo(dbPart.getShelfNo());
                diff.setCacheShelfNo(cacheInfo.getShelfNo());
                diffs.add(diff);
            }
        }
        vo.setDiffs(diffs);

        log.info("规格缓存诊断完成：数据库{}条，缓存{}条，缺失缓存{}个，多余缓存{}个，差异{}个",
                allDbParts.size(), allCacheSpecs.size(), missingInCache.size(), missingInDb.size(), diffs.size());
        return vo;
    }

    public ParsedSpec parseSpec(SmallPart part) {
        return specParser.parse(part.getPartType(), part.getSpecParams());
    }

    public ParsedSpec parseSpec(String partType, String specParams) {
        return specParser.parse(partType, specParams);
    }

    public PinMatrixVO getPinMatrix() {
        List<SmallPart> pins = lambdaQuery()
                .eq(SmallPart::getPartType, "顶针")
                .orderByDesc(SmallPart::getCreateTime)
                .list();

        PinMatrixVO vo = new PinMatrixVO();
        Map<String, PinMatrixVO.PinMatrixCell> cellMap = new LinkedHashMap<>();
        Set<String> materialSet = new LinkedHashSet<>();
        Set<String> diameterSet = new LinkedHashSet<>();
        Set<String> lengthSet = new LinkedHashSet<>();
        int skipped = 0;

        for (SmallPart part : pins) {
            ParsedSpec spec = specParser.parse("顶针", part.getSpecParams());
            if (spec.getDiameter() == null || spec.getLength() == null) {
                skipped++;
                continue;
            }
            String material = (spec.getMaterial() == null || spec.getMaterial().isEmpty()) ? "未指定" : spec.getMaterial();
            String key = material + "|" + spec.getDiameter() + "|" + spec.getLength();
            PinMatrixVO.PinMatrixCell cell = cellMap.get(key);
            if (cell == null) {
                cell = new PinMatrixVO.PinMatrixCell();
                cell.setMaterial(material);
                cell.setDiameter(spec.getDiameter());
                cell.setLength(spec.getLength());
                cell.setQuantity(0);
                cell.setShelfNo("");
                cell.setPartModel("");
                cellMap.put(key, cell);
            }
            int qty = part.getStockQuantity() == null ? 0 : part.getStockQuantity();
            cell.setQuantity(cell.getQuantity() + qty);
            cell.setShelfNo(appendDistinct(cell.getShelfNo(), part.getShelfNo()));
            cell.setPartModel(appendDistinct(cell.getPartModel(), part.getPartModel()));
            materialSet.add(material);
            diameterSet.add(spec.getDiameter());
            lengthSet.add(spec.getLength());
        }

        for (PinMatrixVO.PinMatrixCell cell : cellMap.values()) {
            WarningLevel level = stockThresholdConfigService.calculateWarningLevel(
                    "顶针", cell.getQuantity());
            cell.setWarningLevel(level.getCode());
        }

        List<String> diameters = new ArrayList<>(diameterSet);
        diameters.sort((a, b) -> Double.compare(toDouble(a), toDouble(b)));
        List<String> lengths = new ArrayList<>(lengthSet);
        lengths.sort((a, b) -> Double.compare(toDouble(a), toDouble(b)));
        List<String> materials = new ArrayList<>(materialSet);
        materials.sort((a, b) -> {
            if ("未指定".equals(a)) return 1;
            if ("未指定".equals(b)) return -1;
            return a.compareTo(b);
        });

        vo.setMaterials(materials);
        vo.setDiameters(diameters);
        vo.setLengths(lengths);
        vo.setCells(new ArrayList<>(cellMap.values()));
        vo.setTotalTypes(pins.size());
        vo.setTotalStock(cellMap.values().stream().mapToInt(PinMatrixVO.PinMatrixCell::getQuantity).sum());
        vo.setSkipped(skipped);
        log.info("顶针规格矩阵生成：{} 个型号，{} 个规格组合，{} 个未归类", pins.size(), cellMap.size(), skipped);
        return vo;
    }

    public ShimMatrixVO getShimMatrix() {
        List<SmallPart> shims = lambdaQuery()
                .eq(SmallPart::getPartType, "限位垫片")
                .orderByDesc(SmallPart::getCreateTime)
                .list();

        ShimMatrixVO vo = new ShimMatrixVO();
        Map<String, ShimMatrixVO.ShimMatrixCell> cellMap = new LinkedHashMap<>();
        Set<String> thicknessSet = new LinkedHashSet<>();
        Set<String> outerDiameterSet = new LinkedHashSet<>();
        int skipped = 0;

        for (SmallPart part : shims) {
            ParsedSpec spec = specParser.parse("限位垫片", part.getSpecParams());
            if (spec.getThickness() == null || spec.getOuterDiameter() == null) {
                skipped++;
                continue;
            }
            String key = spec.getThickness() + "|" + spec.getOuterDiameter();
            ShimMatrixVO.ShimMatrixCell cell = cellMap.get(key);
            if (cell == null) {
                cell = new ShimMatrixVO.ShimMatrixCell();
                cell.setThickness(spec.getThickness());
                cell.setOuterDiameter(spec.getOuterDiameter());
                cell.setQuantity(0);
                cell.setShelfNo("");
                cell.setPartModel("");
                cellMap.put(key, cell);
            }
            int qty = part.getStockQuantity() == null ? 0 : part.getStockQuantity();
            cell.setQuantity(cell.getQuantity() + qty);
            cell.setShelfNo(appendDistinct(cell.getShelfNo(), part.getShelfNo()));
            cell.setPartModel(appendDistinct(cell.getPartModel(), part.getPartModel()));
            thicknessSet.add(spec.getThickness());
            outerDiameterSet.add(spec.getOuterDiameter());
        }

        for (ShimMatrixVO.ShimMatrixCell cell : cellMap.values()) {
            WarningLevel level = stockThresholdConfigService.calculateWarningLevel(
                    "限位垫片", cell.getQuantity());
            cell.setWarningLevel(level.getCode());
        }

        List<String> thicknesses = new ArrayList<>(thicknessSet);
        thicknesses.sort((a, b) -> Double.compare(toDouble(a), toDouble(b)));
        List<String> outerDiameters = new ArrayList<>(outerDiameterSet);
        outerDiameters.sort((a, b) -> Double.compare(toDouble(a), toDouble(b)));

        vo.setThicknesses(thicknesses);
        vo.setOuterDiameters(outerDiameters);
        vo.setCells(new ArrayList<>(cellMap.values()));
        vo.setTotalTypes(shims.size());
        vo.setTotalStock(cellMap.values().stream().mapToInt(ShimMatrixVO.ShimMatrixCell::getQuantity).sum());
        vo.setSkipped(skipped);
        log.info("垫片厚度矩阵生成：{} 个型号，{} 个规格组合，{} 个未归类", shims.size(), cellMap.size(), skipped);
        return vo;
    }

    private double toDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (Exception e) {
            return Double.MAX_VALUE;
        }
    }

    private String appendDistinct(String existing, String value) {
        if (value == null || value.isEmpty()) {
            return existing;
        }
        Set<String> set = new LinkedHashSet<>();
        if (existing != null && !existing.isEmpty()) {
            Collections.addAll(set, existing.split(",\\s*"));
        }
        set.add(value);
        return String.join(", ", set);
    }
}
