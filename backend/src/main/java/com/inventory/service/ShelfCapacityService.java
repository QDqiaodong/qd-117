package com.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.dto.ShelfCapacityDTO;
import com.inventory.dto.ShelfSuggestionVO;
import com.inventory.dto.StockInDTO;
import com.inventory.dto.StockInValidationVO;
import com.inventory.entity.ShelfCapacity;
import com.inventory.entity.SmallPart;
import com.inventory.exception.BusinessException;
import com.inventory.mapper.ShelfCapacityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShelfCapacityService extends ServiceImpl<ShelfCapacityMapper, ShelfCapacity> {

    private final ShelfCapacityMapper shelfCapacityMapper;
    private final SmallPartService smallPartService;

    public IPage<ShelfCapacity> getPageList(Integer pageNum, Integer pageSize, String shelfNo) {
        Page<ShelfCapacity> page = new Page<>(pageNum, pageSize);
        return shelfCapacityMapper.selectPageList(page, shelfNo);
    }

    public ShelfCapacity getByShelfNo(String shelfNo) {
        return lambdaQuery().eq(ShelfCapacity::getShelfNo, shelfNo).one();
    }

    public ShelfCapacity getById(Long id) {
        ShelfCapacity capacity = super.getById(id);
        if (capacity == null) {
            throw new BusinessException("货架容量配置不存在");
        }
        return capacity;
    }

    @Transactional(rollbackFor = Exception.class)
    public ShelfCapacity create(ShelfCapacityDTO dto) {
        ShelfCapacity existing = getByShelfNo(dto.getShelfNo());
        if (existing != null) {
            throw new BusinessException("货架编号已存在: " + dto.getShelfNo());
        }
        ShelfCapacity capacity = new ShelfCapacity();
        capacity.setShelfNo(dto.getShelfNo());
        capacity.setMaxPinBoxes(dto.getMaxPinBoxes() != null ? dto.getMaxPinBoxes() : 0);
        capacity.setMaxShimPacks(dto.getMaxShimPacks() != null ? dto.getMaxShimPacks() : 0);
        capacity.setCurrentPinBoxes(0);
        capacity.setCurrentShimPacks(0);
        capacity.setRemark(dto.getRemark());
        capacity.setCreateTime(LocalDateTime.now());
        capacity.setUpdateTime(LocalDateTime.now());
        save(capacity);
        log.info("创建货架容量配置: {}", dto.getShelfNo());
        return capacity;
    }

    @Transactional(rollbackFor = Exception.class)
    public ShelfCapacity update(ShelfCapacityDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        ShelfCapacity capacity = getById(dto.getId());
        if (dto.getShelfNo() != null && !dto.getShelfNo().equals(capacity.getShelfNo())) {
            ShelfCapacity existing = getByShelfNo(dto.getShelfNo());
            if (existing != null && !existing.getId().equals(capacity.getId())) {
                throw new BusinessException("货架编号已存在: " + dto.getShelfNo());
            }
            capacity.setShelfNo(dto.getShelfNo());
        }
        if (dto.getMaxPinBoxes() != null) {
            if (dto.getMaxPinBoxes() < capacity.getCurrentPinBoxes()) {
                throw new BusinessException("顶针最大盒数不能小于当前已存放数量: " + capacity.getCurrentPinBoxes());
            }
            capacity.setMaxPinBoxes(dto.getMaxPinBoxes());
        }
        if (dto.getMaxShimPacks() != null) {
            if (dto.getMaxShimPacks() < capacity.getCurrentShimPacks()) {
                throw new BusinessException("垫片最大包数不能小于当前已存放数量: " + capacity.getCurrentShimPacks());
            }
            capacity.setMaxShimPacks(dto.getMaxShimPacks());
        }
        if (dto.getRemark() != null) {
            capacity.setRemark(dto.getRemark());
        }
        capacity.setUpdateTime(LocalDateTime.now());
        updateById(capacity);
        log.info("更新货架容量配置: {}", capacity.getShelfNo());
        return capacity;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ShelfCapacity capacity = getById(id);
        if ((capacity.getCurrentPinBoxes() != null && capacity.getCurrentPinBoxes() > 0)
                || (capacity.getCurrentShimPacks() != null && capacity.getCurrentShimPacks() > 0)) {
            throw new BusinessException("货架仍有存货，无法删除: " + capacity.getShelfNo());
        }
        removeById(id);
        log.info("删除货架容量配置: {}", capacity.getShelfNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public void increasePinBoxes(String shelfNo, int quantity) {
        ShelfCapacity capacity = getByShelfNo(shelfNo);
        if (capacity == null) {
            log.warn("货架 {} 未配置容量限制，跳过容量校验", shelfNo);
            return;
        }
        int rows = shelfCapacityMapper.increasePinBoxesAtomic(capacity.getId(), quantity);
        if (rows != 1) {
            ShelfCapacity current = getByShelfNo(shelfNo);
            throw new BusinessException("货架 [" + shelfNo + "] 顶针存放数量已达上限，当前: "
                    + current.getCurrentPinBoxes() + "，上限: " + current.getMaxPinBoxes()
                    + "，剩余容量: " + current.getRemainingPinBoxes());
        }
        log.info("货架 {} 顶针盒数 +{}", shelfNo, quantity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void increaseShimPacks(String shelfNo, int quantity) {
        ShelfCapacity capacity = getByShelfNo(shelfNo);
        if (capacity == null) {
            log.warn("货架 {} 未配置容量限制，跳过容量校验", shelfNo);
            return;
        }
        int rows = shelfCapacityMapper.increaseShimPacksAtomic(capacity.getId(), quantity);
        if (rows != 1) {
            ShelfCapacity current = getByShelfNo(shelfNo);
            throw new BusinessException("货架 [" + shelfNo + "] 垫片存放数量已达上限，当前: "
                    + current.getCurrentShimPacks() + "，上限: " + current.getMaxShimPacks()
                    + "，剩余容量: " + current.getRemainingShimPacks());
        }
        log.info("货架 {} 垫片包数 +{}", shelfNo, quantity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void decreasePinBoxes(String shelfNo, int quantity) {
        ShelfCapacity capacity = getByShelfNo(shelfNo);
        if (capacity == null) {
            return;
        }
        shelfCapacityMapper.decreasePinBoxesAtomic(capacity.getId(), quantity);
        log.info("货架 {} 顶针盒数 -{}", shelfNo, quantity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void decreaseShimPacks(String shelfNo, int quantity) {
        ShelfCapacity capacity = getByShelfNo(shelfNo);
        if (capacity == null) {
            return;
        }
        shelfCapacityMapper.decreaseShimPacksAtomic(capacity.getId(), quantity);
        log.info("货架 {} 垫片包数 -{}", shelfNo, quantity);
    }

    public StockInValidationVO validateStockIn(StockInDTO dto) {
        StockInValidationVO result = StockInValidationVO.success();

        Map<String, Integer> pinBoxesByShelf = new HashMap<>();
        Map<String, Integer> shimPacksByShelf = new HashMap<>();
        Map<String, List<String>> shelfItemLabels = new HashMap<>();

        for (StockInDTO.StockInItem item : dto.getItems()) {
            String shelfNo = item.getShelfNo();
            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
            String label = item.getPartModel() + "(" + quantity + ")";

            if ("顶针".equals(item.getPartType())) {
                SmallPart existing = smallPartService.getByModel(item.getPartModel());
                int additionalQty = (existing != null && existing.getShelfNo() != null
                        && existing.getShelfNo().equals(shelfNo)) ? quantity : quantity;
                pinBoxesByShelf.merge(shelfNo, additionalQty, Integer::sum);
                shelfItemLabels.computeIfAbsent(shelfNo + "_顶针", k -> new ArrayList<>()).add(label);
            } else if ("限位垫片".equals(item.getPartType())) {
                SmallPart existing = smallPartService.getByModel(item.getPartModel());
                int additionalQty = (existing != null && existing.getShelfNo() != null
                        && existing.getShelfNo().equals(shelfNo)) ? quantity : quantity;
                shimPacksByShelf.merge(shelfNo, additionalQty, Integer::sum);
                shelfItemLabels.computeIfAbsent(shelfNo + "_限位垫片", k -> new ArrayList<>()).add(label);
            }
        }

        for (Map.Entry<String, Integer> entry : pinBoxesByShelf.entrySet()) {
            String shelfNo = entry.getKey();
            int totalAdditional = entry.getValue();
            ShelfCapacity capacity = getByShelfNo(shelfNo);
            if (capacity == null) {
                log.warn("货架 {} 未配置容量限制，跳过容量校验", shelfNo);
                continue;
            }
            if (!capacity.canAddPinBoxes(totalAdditional)) {
                String itemDetail = String.join(" + ", shelfItemLabels.getOrDefault(shelfNo + "_顶针", new ArrayList<>()));
                result.addError("货架 [" + shelfNo + "] 顶针容量不足，本批合计需要 " + totalAdditional
                        + " 盒（" + itemDetail + "），剩余容量仅 " + capacity.getRemainingPinBoxes() + " 盒");
                result.getSuggestions().addAll(findAvailableShelvesForPin(totalAdditional));
            }
        }

        for (Map.Entry<String, Integer> entry : shimPacksByShelf.entrySet()) {
            String shelfNo = entry.getKey();
            int totalAdditional = entry.getValue();
            ShelfCapacity capacity = getByShelfNo(shelfNo);
            if (capacity == null) {
                log.warn("货架 {} 未配置容量限制，跳过容量校验", shelfNo);
                continue;
            }
            if (!capacity.canAddShimPacks(totalAdditional)) {
                String itemDetail = String.join(" + ", shelfItemLabels.getOrDefault(shelfNo + "_限位垫片", new ArrayList<>()));
                result.addError("货架 [" + shelfNo + "] 垫片容量不足，本批合计需要 " + totalAdditional
                        + " 包（" + itemDetail + "），剩余容量仅 " + capacity.getRemainingShimPacks() + " 包");
                result.getSuggestions().addAll(findAvailableShelvesForShim(totalAdditional));
            }
        }

        return result;
    }

    public List<ShelfSuggestionVO> findAvailableShelvesForPin(int requiredQuantity) {
        List<ShelfCapacity> allCapacities = list();
        return allCapacities.stream()
                .filter(c -> c.getMaxPinBoxes() != null && c.getMaxPinBoxes() > 0)
                .filter(c -> c.getRemainingPinBoxes() >= requiredQuantity)
                .sorted(Comparator.comparingInt(ShelfCapacity::getRemainingPinBoxes).reversed())
                .map(c -> {
                    ShelfSuggestionVO vo = new ShelfSuggestionVO();
                    vo.setShelfNo(c.getShelfNo());
                    vo.setRemainingCapacity(c.getRemainingPinBoxes());
                    vo.setMaxCapacity(c.getMaxPinBoxes());
                    vo.setPartType("顶针");
                    vo.setRemark(c.getRemark());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    public List<ShelfSuggestionVO> findAvailableShelvesForShim(int requiredQuantity) {
        List<ShelfCapacity> allCapacities = list();
        return allCapacities.stream()
                .filter(c -> c.getMaxShimPacks() != null && c.getMaxShimPacks() > 0)
                .filter(c -> c.getRemainingShimPacks() >= requiredQuantity)
                .sorted(Comparator.comparingInt(ShelfCapacity::getRemainingShimPacks).reversed())
                .map(c -> {
                    ShelfSuggestionVO vo = new ShelfSuggestionVO();
                    vo.setShelfNo(c.getShelfNo());
                    vo.setRemainingCapacity(c.getRemainingShimPacks());
                    vo.setMaxCapacity(c.getMaxShimPacks());
                    vo.setPartType("限位垫片");
                    vo.setRemark(c.getRemark());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    public List<ShelfSuggestionVO> getAvailableShelves(String partType, Integer requiredQuantity) {
        int qty = requiredQuantity != null ? requiredQuantity : 1;
        if ("顶针".equals(partType)) {
            return findAvailableShelvesForPin(qty);
        } else if ("限位垫片".equals(partType)) {
            return findAvailableShelvesForShim(qty);
        }
        return new ArrayList<>();
    }

    public void recalculateCurrentUsage(String shelfNo) {
        ShelfCapacity capacity = getByShelfNo(shelfNo);
        if (capacity == null) {
            return;
        }
        List<SmallPart> partsOnShelf = smallPartService.lambdaQuery()
                .eq(SmallPart::getShelfNo, shelfNo)
                .list();
        int pinBoxes = 0;
        int shimPacks = 0;
        for (SmallPart part : partsOnShelf) {
            if ("顶针".equals(part.getPartType())) {
                pinBoxes += part.getStockQuantity() != null ? part.getStockQuantity() : 0;
            } else if ("限位垫片".equals(part.getPartType())) {
                shimPacks += part.getStockQuantity() != null ? part.getStockQuantity() : 0;
            }
        }
        capacity.setCurrentPinBoxes(pinBoxes);
        capacity.setCurrentShimPacks(shimPacks);
        capacity.setUpdateTime(LocalDateTime.now());
        updateById(capacity);
        log.info("重新计算货架 {} 使用量：顶针 {} 盒，垫片 {} 包", shelfNo, pinBoxes, shimPacks);
    }
}
