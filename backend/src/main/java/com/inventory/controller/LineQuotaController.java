package com.inventory.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.inventory.common.Result;
import com.inventory.dto.LineQuotaDTO;
import com.inventory.dto.QuotaCheckDTO;
import com.inventory.dto.QuotaCheckResultVO;
import com.inventory.entity.LineQuota;
import com.inventory.service.LineQuotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/line-quota")
@RequiredArgsConstructor
public class LineQuotaController {

    private final LineQuotaService lineQuotaService;

    @GetMapping("/page")
    public Result<IPage<LineQuota>> getPageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) String productionLine,
            @RequestParam(required = false) String partType) {
        return Result.success(lineQuotaService.getPageList(pageNum, pageSize, quarter, productionLine, partType));
    }

    @GetMapping("/{id}")
    public Result<LineQuota> getById(@PathVariable Long id) {
        return Result.success(lineQuotaService.getById(id));
    }

    @GetMapping("/list")
    public Result<List<LineQuota>> listAll() {
        return Result.success(lineQuotaService.listAll());
    }

    @GetMapping("/current-quarter")
    public Result<String> currentQuarter() {
        return Result.success(lineQuotaService.currentQuarter());
    }

    @GetMapping("/enums")
    public Result<Map<String, List<String>>> enums() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("productionLines", LineQuotaService.PRODUCTION_LINES);
        map.put("partTypes", LineQuotaService.PART_TYPES);
        return Result.success(map);
    }

    @PostMapping("/check")
    public Result<QuotaCheckResultVO> checkQuota(@Valid @RequestBody QuotaCheckDTO dto) {
        return Result.success(lineQuotaService.checkQuota(dto));
    }

    @PostMapping
    public Result<LineQuota> create(@Valid @RequestBody LineQuotaDTO dto) {
        return Result.success(lineQuotaService.create(dto));
    }

    @PutMapping
    public Result<LineQuota> update(@Valid @RequestBody LineQuotaDTO dto) {
        return Result.success(lineQuotaService.update(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        lineQuotaService.delete(id);
        return Result.success();
    }

    @PostMapping("/recalculate")
    public Result<Void> recalculate(@RequestParam String quarter,
                                     @RequestParam String productionLine,
                                     @RequestParam String partType) {
        lineQuotaService.recalculateUsedQuantity(quarter, productionLine, partType);
        return Result.success();
    }
}
