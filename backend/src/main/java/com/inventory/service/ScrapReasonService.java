package com.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.dto.ScrapReasonDTO;
import com.inventory.entity.ScrapReason;
import com.inventory.exception.BusinessException;
import com.inventory.mapper.ScrapReasonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapReasonService extends ServiceImpl<ScrapReasonMapper, ScrapReason> {

    private final ScrapReasonMapper scrapReasonMapper;

    public IPage<ScrapReason> getPageList(Integer pageNum, Integer pageSize, String reasonName, String partType, Integer status) {
        Page<ScrapReason> page = new Page<>(pageNum, pageSize);
        return scrapReasonMapper.selectPageList(page, reasonName, partType, status);
    }

    public ScrapReason getById(Long id) {
        ScrapReason reason = super.getById(id);
        if (reason == null) {
            throw new BusinessException("破损原因不存在");
        }
        return reason;
    }

    public List<ScrapReason> listEnabled() {
        return lambdaQuery()
                .eq(ScrapReason::getStatus, 1)
                .orderByAsc(ScrapReason::getSort)
                .list();
    }

    public List<ScrapReason> listByPartType(String partType) {
        return lambdaQuery()
                .eq(ScrapReason::getStatus, 1)
                .and(wrapper -> wrapper
                        .eq(ScrapReason::getPartType, partType)
                        .or()
                        .eq(ScrapReason::getPartType, "全部"))
                .orderByAsc(ScrapReason::getSort)
                .list();
    }

    @Transactional(rollbackFor = Exception.class)
    public ScrapReason create(ScrapReasonDTO dto) {
        ScrapReason existingCode = lambdaQuery()
                .eq(ScrapReason::getReasonCode, dto.getReasonCode())
                .one();
        if (existingCode != null) {
            throw new BusinessException("原因编码已存在: " + dto.getReasonCode());
        }
        ScrapReason existingName = lambdaQuery()
                .eq(ScrapReason::getReasonName, dto.getReasonName())
                .one();
        if (existingName != null) {
            throw new BusinessException("原因名称已存在: " + dto.getReasonName());
        }

        ScrapReason reason = new ScrapReason();
        reason.setReasonName(dto.getReasonName());
        reason.setReasonCode(dto.getReasonCode());
        reason.setPartType(dto.getPartType());
        reason.setSort(dto.getSort());
        reason.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        reason.setRemark(dto.getRemark());
        reason.setCreateTime(LocalDateTime.now());
        reason.setUpdateTime(LocalDateTime.now());
        save(reason);
        log.info("创建破损原因: {}", dto.getReasonName());
        return reason;
    }

    @Transactional(rollbackFor = Exception.class)
    public ScrapReason update(ScrapReasonDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        ScrapReason reason = getById(dto.getId());

        if (dto.getReasonCode() != null && !dto.getReasonCode().equals(reason.getReasonCode())) {
            ScrapReason existingCode = lambdaQuery()
                    .eq(ScrapReason::getReasonCode, dto.getReasonCode())
                    .ne(ScrapReason::getId, dto.getId())
                    .one();
            if (existingCode != null) {
                throw new BusinessException("原因编码已存在: " + dto.getReasonCode());
            }
            reason.setReasonCode(dto.getReasonCode());
        }
        if (dto.getReasonName() != null && !dto.getReasonName().equals(reason.getReasonName())) {
            ScrapReason existingName = lambdaQuery()
                    .eq(ScrapReason::getReasonName, dto.getReasonName())
                    .ne(ScrapReason::getId, dto.getId())
                    .one();
            if (existingName != null) {
                throw new BusinessException("原因名称已存在: " + dto.getReasonName());
            }
            reason.setReasonName(dto.getReasonName());
        }
        if (dto.getPartType() != null) {
            reason.setPartType(dto.getPartType());
        }
        if (dto.getSort() != null) {
            reason.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            reason.setStatus(dto.getStatus());
        }
        if (dto.getRemark() != null) {
            reason.setRemark(dto.getRemark());
        }
        reason.setUpdateTime(LocalDateTime.now());
        updateById(reason);
        log.info("更新破损原因: {}", reason.getReasonName());
        return reason;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ScrapReason reason = getById(id);
        removeById(id);
        log.info("删除破损原因: {}", reason.getReasonName());
    }

    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        ScrapReason reason = getById(id);
        Integer newStatus = reason.getStatus() == 1 ? 0 : 1;
        reason.setStatus(newStatus);
        reason.setUpdateTime(LocalDateTime.now());
        updateById(reason);
        log.info("切换破损原因状态: {} -> {}", reason.getReasonName(), newStatus == 1 ? "启用" : "禁用");
    }
}
