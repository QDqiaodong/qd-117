package com.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.dto.LineQuotaDTO;
import com.inventory.dto.QuotaCheckDTO;
import com.inventory.dto.QuotaCheckResultVO;
import com.inventory.dto.StockOutDTO;
import com.inventory.entity.LineQuota;
import com.inventory.entity.SmallPart;
import com.inventory.exception.BusinessException;
import com.inventory.mapper.LineQuotaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LineQuotaService extends ServiceImpl<LineQuotaMapper, LineQuota> {

    public static final List<String> PRODUCTION_LINES =
            Arrays.asList("一号装配线", "二号装配线", "三号装配线", "四号装配线");

    public static final List<String> PART_TYPES = Arrays.asList("顶针", "限位垫片");

    private final LineQuotaMapper lineQuotaMapper;
    private final SmallPartService smallPartService;

    public IPage<LineQuota> getPageList(Integer pageNum, Integer pageSize,
                                          String quarter, String productionLine, String partType) {
        Page<LineQuota> page = new Page<>(pageNum, pageSize);
        return lineQuotaMapper.selectPageList(page, quarter, productionLine, partType);
    }

    public LineQuota getById(Long id) {
        LineQuota quota = super.getById(id);
        if (quota == null) {
            throw new BusinessException("产线领用配额不存在");
        }
        return quota;
    }

    public LineQuota getByQuarterLineType(String quarter, String productionLine, String partType) {
        return lambdaQuery()
                .eq(LineQuota::getQuarter, quarter)
                .eq(LineQuota::getProductionLine, productionLine)
                .eq(LineQuota::getPartType, partType)
                .one();
    }

    @Transactional(rollbackFor = Exception.class)
    public LineQuota create(LineQuotaDTO dto) {
        validateEnum(dto.getProductionLine(), dto.getPartType());
        LineQuota existing = getByQuarterLineType(dto.getQuarter(), dto.getProductionLine(), dto.getPartType());
        if (existing != null) {
            throw new BusinessException("配额已存在: " + dto.getQuarter() + " " + dto.getProductionLine() + " " + dto.getPartType());
        }
        LineQuota quota = new LineQuota();
        quota.setQuarter(dto.getQuarter());
        quota.setProductionLine(dto.getProductionLine());
        quota.setPartType(dto.getPartType());
        quota.setMaxQuantity(dto.getMaxQuantity() != null ? dto.getMaxQuantity() : 0);
        quota.setUsedQuantity(0);
        quota.setRemark(dto.getRemark());
        quota.setCreateTime(LocalDateTime.now());
        quota.setUpdateTime(LocalDateTime.now());
        save(quota);
        log.info("创建产线领用配额: {} {} {} 上限={}", dto.getQuarter(), dto.getProductionLine(), dto.getPartType(), dto.getMaxQuantity());
        return quota;
    }

    @Transactional(rollbackFor = Exception.class)
    public LineQuota update(LineQuotaDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        LineQuota quota = getById(dto.getId());
        if (dto.getProductionLine() != null || dto.getPartType() != null || dto.getQuarter() != null) {
            String quarter = dto.getQuarter() != null ? dto.getQuarter() : quota.getQuarter();
            String line = dto.getProductionLine() != null ? dto.getProductionLine() : quota.getProductionLine();
            String partType = dto.getPartType() != null ? dto.getPartType() : quota.getPartType();
            validateEnum(line, partType);
            LineQuota existing = getByQuarterLineType(quarter, line, partType);
            if (existing != null && !existing.getId().equals(quota.getId())) {
                throw new BusinessException("配额已存在: " + quarter + " " + line + " " + partType);
            }
            quota.setQuarter(quarter);
            quota.setProductionLine(line);
            quota.setPartType(partType);
        }
        if (dto.getMaxQuantity() != null) {
            int used = quota.getUsedQuantity() == null ? 0 : quota.getUsedQuantity();
            if (dto.getMaxQuantity() < used) {
                throw new BusinessException("配额上限不能小于当前已领用数量: " + used);
            }
            quota.setMaxQuantity(dto.getMaxQuantity());
        }
        if (dto.getRemark() != null) {
            quota.setRemark(dto.getRemark());
        }
        quota.setUpdateTime(LocalDateTime.now());
        updateById(quota);
        log.info("更新产线领用配额: {} {} {}", quota.getQuarter(), quota.getProductionLine(), quota.getPartType());
        return quota;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        LineQuota quota = getById(id);
        removeById(id);
        log.info("删除产线领用配额: {} {} {}", quota.getQuarter(), quota.getProductionLine(), quota.getPartType());
    }

    @Transactional(rollbackFor = Exception.class)
    public void consumeQuota(String productionLine, String partType, int quantity) {
        String quarter = currentQuarter();
        LineQuota quota = getByQuarterLineType(quarter, productionLine, partType);
        if (quota == null) {
            log.warn("未配置 {} {} {} 季度领用配额，跳过配额校验", quarter, productionLine, partType);
            return;
        }
        int rows = lineQuotaMapper.consumeQuotaAtomic(quota.getId(), quantity);
        if (rows != 1) {
            LineQuota current = getByQuarterLineType(quarter, productionLine, partType);
            int remaining = current.getRemainingQuantity();
            int exceeded = Math.max(0, quantity - remaining);
            throw new BusinessException("产线领用配额不足: [" + quarter + " " + productionLine + " " + partType + "] "
                    + "本次申请 " + quantity + "，配额剩余 " + remaining + "，超出 " + exceeded);
        }
        log.info("消耗配额 {} {} {} +{}", quarter, productionLine, partType, quantity);
    }

    public QuotaCheckResultVO checkQuota(QuotaCheckDTO dto) {
        String quarter = currentQuarter();
        Map<String, Integer> typeQtyMap = new HashMap<>();
        for (StockOutDTO.StockOutItem item : dto.getItems()) {
            SmallPart part = smallPartService.getById(item.getPartId());
            int qty = item.getQuantity() == null ? 0 : item.getQuantity();
            typeQtyMap.merge(part.getPartType(), qty, Integer::sum);
        }

        QuotaCheckResultVO result = new QuotaCheckResultVO();
        result.setQuarter(quarter);
        result.setProductionLine(dto.getProductionLine());
        boolean passed = true;

        for (String partType : PART_TYPES) {
            int requested = typeQtyMap.getOrDefault(partType, 0);
            QuotaCheckResultVO.QuotaDetail detail = new QuotaCheckResultVO.QuotaDetail();
            detail.setPartType(partType);
            detail.setRequestedQuantity(requested);
            LineQuota quota = getByQuarterLineType(quarter, dto.getProductionLine(), partType);
            if (quota == null) {
                detail.setConfigured(false);
                detail.setMaxQuantity(null);
                detail.setUsedQuantity(0);
                detail.setRemainingQuantity(null);
                detail.setExceededQuantity(0);
                detail.setExceeded(false);
            } else {
                detail.setConfigured(true);
                detail.setMaxQuantity(quota.getMaxQuantity());
                detail.setUsedQuantity(quota.getUsedQuantity());
                int remaining = quota.getRemainingQuantity();
                detail.setRemainingQuantity(remaining);
                int exceeded = Math.max(0, requested - remaining);
                detail.setExceededQuantity(exceeded);
                boolean exceededFlag = requested > remaining;
                detail.setExceeded(exceededFlag);
                if (exceededFlag) {
                    passed = false;
                }
            }
            result.getDetails().add(detail);
        }

        result.setPassed(passed);
        result.setMessage(passed ? "配额校验通过" : "存在超额领用，请调整数量后再提交");
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void recalculateUsedQuantity(String quarter, String productionLine, String partType) {
        LineQuota quota = getByQuarterLineType(quarter, productionLine, partType);
        if (quota == null) {
            throw new BusinessException("配额不存在: " + quarter + " " + productionLine + " " + partType);
        }
        String[] range = quarterDateRange(quarter);
        int sum = lineQuotaMapper.sumUsedFromRecords(productionLine, partType, range[0], range[1]);
        quota.setUsedQuantity(sum);
        quota.setUpdateTime(LocalDateTime.now());
        updateById(quota);
        log.info("重新计算配额已领用量: {} {} {} = {}", quarter, productionLine, partType, sum);
    }

    public List<LineQuota> listAll() {
        return list();
    }

    public String currentQuarter() {
        return currentQuarter(LocalDate.now());
    }

    public static String currentQuarter(LocalDate date) {
        int q = (date.getMonthValue() - 1) / 3 + 1;
        return date.getYear() + "-Q" + q;
    }

    public static String[] quarterDateRange(String quarter) {
        String[] parts = quarter.split("-Q");
        int year = Integer.parseInt(parts[0]);
        int q = Integer.parseInt(parts[1]);
        int startMonth = (q - 1) * 3 + 1;
        LocalDate start = LocalDate.of(year, startMonth, 1);
        LocalDate end = start.plusMonths(3).minusDays(1);
        return new String[]{start.atStartOfDay().toString().replace("T", " "),
                end.atTime(23, 59, 59).toString().replace("T", " ")};
    }

    private void validateEnum(String productionLine, String partType) {
        if (productionLine != null && !PRODUCTION_LINES.contains(productionLine)) {
            throw new BusinessException("无效的领用产线: " + productionLine);
        }
        if (partType != null && !PART_TYPES.contains(partType)) {
            throw new BusinessException("无效的小件类型: " + partType);
        }
    }
}
