package com.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.WarningLevel;
import com.inventory.entity.StockThresholdConfig;
import com.inventory.exception.BusinessException;
import com.inventory.mapper.StockThresholdConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockThresholdConfigService extends ServiceImpl<StockThresholdConfigMapper, StockThresholdConfig> {

    private final StockThresholdConfigMapper stockThresholdConfigMapper;

    private final Map<String, StockThresholdConfig> configCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    public void refreshCache() {
        List<StockThresholdConfig> configs = list();
        configCache.clear();
        for (StockThresholdConfig config : configs) {
            configCache.put(config.getPartType(), config);
        }
        log.info("库存阈值配置缓存刷新完成，共 {} 条记录", configs.size());
    }

    public StockThresholdConfig getByPartType(String partType) {
        StockThresholdConfig config = configCache.get(partType);
        if (config == null) {
            config = lambdaQuery().eq(StockThresholdConfig::getPartType, partType).one();
            if (config != null) {
                configCache.put(partType, config);
            }
        }
        return config;
    }

    public WarningLevel calculateWarningLevel(String partType, Integer stockQuantity) {
        if (stockQuantity == null) {
            return WarningLevel.NORMAL;
        }
        StockThresholdConfig config = getByPartType(partType);
        if (config == null) {
            return WarningLevel.NORMAL;
        }
        if (stockQuantity <= config.getDangerThreshold()) {
            return WarningLevel.DANGER;
        }
        if (stockQuantity <= config.getWarningThreshold()) {
            return WarningLevel.WARNING;
        }
        return WarningLevel.NORMAL;
    }

    public List<StockThresholdConfig> listAll() {
        return list();
    }

    public StockThresholdConfig create(StockThresholdConfig config) {
        if (config.getPartType() != null) {
            config.setPartType(config.getPartType().trim());
        }
        if (config.getRemark() != null) {
            config.setRemark(config.getRemark().trim());
        }
        validateThresholds(config);
        StockThresholdConfig existing = lambdaQuery()
                .eq(StockThresholdConfig::getPartType, config.getPartType())
                .one();
        if (existing != null) {
            throw new BusinessException("该零件类型的阈值配置已存在: " + config.getPartType());
        }
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        save(config);
        refreshCache();
        log.info("创建库存阈值配置: {}", config.getPartType());
        return config;
    }

    public StockThresholdConfig update(StockThresholdConfig config) {
        StockThresholdConfig oldConfig = getById(config.getId());
        if (oldConfig == null) {
            throw new BusinessException("阈值配置不存在");
        }
        if (config.getRemark() != null) {
            config.setRemark(config.getRemark().trim());
        }
        validateThresholds(config);
        config.setUpdateTime(LocalDateTime.now());
        updateById(config);
        refreshCache();
        log.info("更新库存阈值配置: {}", config.getPartType());
        return getById(config.getId());
    }

    public void delete(Long id) {
        StockThresholdConfig config = getById(id);
        if (config == null) {
            throw new BusinessException("阈值配置不存在");
        }
        removeById(id);
        refreshCache();
        log.info("删除库存阈值配置: {}", config.getPartType());
    }

    private void validateThresholds(StockThresholdConfig config) {
        if (config.getDangerThreshold() == null || config.getDangerThreshold() < 0) {
            throw new BusinessException("危险阈值不能为负数");
        }
        if (config.getWarningThreshold() == null || config.getWarningThreshold() < 0) {
            throw new BusinessException("警告阈值不能为负数");
        }
        if (config.getDangerThreshold() > config.getWarningThreshold()) {
            throw new BusinessException("危险阈值不能大于警告阈值");
        }
    }
}
