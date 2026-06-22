package com.inventory.controller;

import com.inventory.common.Result;
import com.inventory.entity.StockThresholdConfig;
import com.inventory.service.StockThresholdConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock-threshold")
@RequiredArgsConstructor
public class StockThresholdConfigController {

    private final StockThresholdConfigService stockThresholdConfigService;

    @GetMapping("/list")
    public Result<List<StockThresholdConfig>> list() {
        return Result.success(stockThresholdConfigService.listAll());
    }

    @GetMapping("/{id}")
    public Result<StockThresholdConfig> getById(@PathVariable Long id) {
        return Result.success(stockThresholdConfigService.getById(id));
    }

    @GetMapping("/by-part-type")
    public Result<StockThresholdConfig> getByPartType(@RequestParam String partType) {
        return Result.success(stockThresholdConfigService.getByPartType(partType));
    }

    @PostMapping
    public Result<StockThresholdConfig> create(@Valid @RequestBody StockThresholdConfig config) {
        return Result.success(stockThresholdConfigService.create(config));
    }

    @PutMapping
    public Result<StockThresholdConfig> update(@Valid @RequestBody StockThresholdConfig config) {
        return Result.success(stockThresholdConfigService.update(config));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        stockThresholdConfigService.delete(id);
        return Result.success();
    }

    @PostMapping("/refresh-cache")
    public Result<Void> refreshCache() {
        stockThresholdConfigService.refreshCache();
        return Result.success();
    }
}
