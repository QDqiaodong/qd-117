package com.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.cache.PartSpecCache;
import com.inventory.entity.SmallPart;
import com.inventory.exception.BusinessException;
import com.inventory.mapper.SmallPartMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmallPartService extends ServiceImpl<SmallPartMapper, SmallPart> {

    private final SmallPartMapper smallPartMapper;
    private final PartSpecCache partSpecCache;

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
}
