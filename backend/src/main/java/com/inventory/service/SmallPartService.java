package com.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.cache.PartSpecCache;
import com.inventory.dto.PinMatrixVO;
import com.inventory.entity.SmallPart;
import com.inventory.exception.BusinessException;
import com.inventory.mapper.SmallPartMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmallPartService extends ServiceImpl<SmallPartMapper, SmallPart> {

    private final SmallPartMapper smallPartMapper;
    private final PartSpecCache partSpecCache;
    private final ObjectMapper objectMapper;

    public IPage<SmallPart> getPageList(Integer pageNum, Integer pageSize, String partType, String keyword) {
        Page<SmallPart> page = new Page<>(pageNum, pageSize);
        return smallPartMapper.selectPageList(page, partType, keyword);
    }

    public SmallPart getById(Long id) {
        SmallPart part = super.getById(id);
        if (part == null) {
            throw new BusinessException("小件不存在");
        }
        return part;
    }

    public SmallPart getByModel(String partModel) {
        return lambdaQuery().eq(SmallPart::getPartModel, partModel).one();
    }

    @Transactional(rollbackFor = Exception.class)
    public SmallPart create(SmallPart part) {
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

    @Transactional(rollbackFor = Exception.class)
    public SmallPart update(SmallPart part) {
        SmallPart oldPart = getById(part.getId());
        part.setUpdateTime(LocalDateTime.now());
        updateById(part);
        SmallPart updated = getById(part.getId());
        partSpecCache.updatePartSpec(oldPart, updated);
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
    public void increaseStock(Long id, Integer quantity) {
        SmallPart part = getById(id);
        part.setStockQuantity(part.getStockQuantity() + quantity);
        part.setUpdateTime(LocalDateTime.now());
        updateById(part);
        SmallPart updated = getById(id);
        partSpecCache.updatePartSpec(part, updated);
    }

    @Transactional(rollbackFor = Exception.class)
    public void decreaseStock(Long id, Integer quantity) {
        SmallPart part = getById(id);
        if (part.getStockQuantity() < quantity) {
            throw new BusinessException("库存不足: " + part.getPartModel() + ", 当前库存: " + part.getStockQuantity());
        }
        part.setStockQuantity(part.getStockQuantity() - quantity);
        part.setUpdateTime(LocalDateTime.now());
        updateById(part);
        SmallPart updated = getById(id);
        partSpecCache.updatePartSpec(part, updated);
    }

    public List<SmallPart> listAll() {
        return list();
    }

    public void refreshCache() {
        partSpecCache.clearCache();
        List<SmallPart> parts = list();
        for (SmallPart part : parts) {
            partSpecCache.addPartSpec(part);
        }
        log.info("小件规格缓存刷新完成，共 {} 条记录", parts.size());
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
            PinSpec spec = parsePinSpec(part.getSpecParams());
            if (spec.diameter == null || spec.length == null) {
                skipped++;
                continue;
            }
            String material = (spec.material == null || spec.material.isEmpty()) ? "未指定" : spec.material;
            String key = material + "|" + spec.diameter + "|" + spec.length;
            PinMatrixVO.PinMatrixCell cell = cellMap.get(key);
            if (cell == null) {
                cell = new PinMatrixVO.PinMatrixCell();
                cell.setMaterial(material);
                cell.setDiameter(spec.diameter);
                cell.setLength(spec.length);
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
            diameterSet.add(spec.diameter);
            lengthSet.add(spec.length);
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

    private PinSpec parsePinSpec(String specParams) {
        PinSpec spec = new PinSpec();
        if (specParams == null || specParams.trim().isEmpty()) {
            return spec;
        }
        String raw = specParams.trim();
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node != null && node.isObject()) {
                spec.diameter = stripToNumber(getJsonAny(node, "diameter", "直径", "Diameter", "D"));
                spec.length = stripToNumber(getJsonAny(node, "length", "长度", "Length", "L"));
                String material = getJsonAny(node, "material", "材质", "Material", "M");
                spec.material = material == null ? null : material.trim();
            }
        } catch (Exception ignored) {
        }
        if (spec.diameter == null) {
            spec.diameter = extractNumber(raw, "直径");
        }
        if (spec.length == null) {
            spec.length = extractNumber(raw, "长度");
        }
        if (spec.material == null || spec.material.isEmpty()) {
            String material = extractMaterial(raw);
            spec.material = material == null ? null : material.trim();
        }
        return spec;
    }

    private String getJsonAny(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) {
                String text = value.isTextual() ? value.asText() : value.toString();
                if (text != null && !text.trim().isEmpty()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private String stripToNumber(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : (text.trim().isEmpty() ? null : text.trim());
    }

    private String extractNumber(String text, String keyword) {
        Matcher matcher = Pattern.compile(keyword + "\\s*[:：]?\\s*(\\d+(?:\\.\\d+)?)").matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractMaterial(String text) {
        Matcher matcher = MATERIAL_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
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

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final Pattern MATERIAL_PATTERN = Pattern.compile("材质\\s*[:：]?\\s*([A-Za-z0-9\\u4e00-\\u9fa5_\\-]+)");

    private static class PinSpec {
        private String diameter;
        private String length;
        private String material;
    }
}
