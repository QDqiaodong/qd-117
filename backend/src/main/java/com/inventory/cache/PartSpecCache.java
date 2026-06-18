package com.inventory.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.entity.SmallPart;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartSpecCache {

    private static final String PART_SPECS_KEY = "inventory:part:specs";
    private static final String PART_TYPE_PREFIX = "inventory:part:type:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private ZSetOperations<String, String> zSetOps;

    @PostConstruct
    public void init() {
        zSetOps = stringRedisTemplate.opsForZSet();
    }

    public void addPartSpec(SmallPart part) {
        try {
            String specJson = objectMapper.writeValueAsString(buildSpecInfo(part));
            zSetOps.add(PART_SPECS_KEY, specJson, getPartScore(part));
            zSetOps.add(PART_TYPE_PREFIX + part.getPartType(), specJson, getPartScore(part));
            log.info("小件规格已缓存: {}", part.getPartModel());
        } catch (JsonProcessingException e) {
            log.error("序列化小件规格失败", e);
        }
    }

    public void removePartSpec(SmallPart part) {
        try {
            String specJson = objectMapper.writeValueAsString(buildSpecInfo(part));
            zSetOps.remove(PART_SPECS_KEY, specJson);
            zSetOps.remove(PART_TYPE_PREFIX + part.getPartType(), specJson);
            log.info("小件规格已从缓存移除: {}", part.getPartModel());
        } catch (JsonProcessingException e) {
            log.error("序列化小件规格失败", e);
        }
    }

    public void updatePartSpec(SmallPart oldPart, SmallPart newPart) {
        removePartSpec(oldPart);
        addPartSpec(newPart);
    }

    public List<PartSpecInfo> getAllSpecs() {
        Set<String> specs = zSetOps.reverseRange(PART_SPECS_KEY, 0, -1);
        return parseSpecs(specs);
    }

    public List<PartSpecInfo> getSpecsByType(String partType) {
        Set<String> specs = zSetOps.reverseRange(PART_TYPE_PREFIX + partType, 0, -1);
        return parseSpecs(specs);
    }

    public List<PartSpecInfo> getSpecsByRange(long start, long end) {
        Set<String> specs = zSetOps.reverseRange(PART_SPECS_KEY, start, end);
        return parseSpecs(specs);
    }

    public void clearCache() {
        stringRedisTemplate.delete(PART_SPECS_KEY);
        Set<String> keys = stringRedisTemplate.keys(PART_TYPE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
        log.info("小件规格缓存已清空");
    }

    private double getPartScore(SmallPart part) {
        return part.getUpdateTime() != null ? part.getUpdateTime().toEpochSecond(java.time.ZoneOffset.of("+8")) : System.currentTimeMillis() / 1000.0;
    }

    private PartSpecInfo buildSpecInfo(SmallPart part) {
        PartSpecInfo info = new PartSpecInfo();
        info.setId(part.getId());
        info.setPartModel(part.getPartModel());
        info.setPartName(part.getPartName());
        info.setPartType(part.getPartType());
        info.setSpecParams(part.getSpecParams());
        info.setShelfNo(part.getShelfNo());
        info.setUnit(part.getUnit());
        info.setStockQuantity(part.getStockQuantity());
        return info;
    }

    private List<PartSpecInfo> parseSpecs(Set<String> specs) {
        List<PartSpecInfo> result = new ArrayList<>();
        if (specs == null || specs.isEmpty()) {
            return result;
        }
        for (String specJson : specs) {
            try {
                PartSpecInfo info = objectMapper.readValue(specJson, PartSpecInfo.class);
                result.add(info);
            } catch (JsonProcessingException e) {
                log.error("解析小件规格缓存失败: {}", specJson, e);
            }
        }
        return result;
    }

    @lombok.Data
    public static class PartSpecInfo {
        private Long id;
        private String partModel;
        private String partName;
        private String partType;
        private String specParams;
        private String shelfNo;
        private String unit;
        private Integer stockQuantity;
    }
}
