package com.inventory.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.inventory.common.Result;
import com.inventory.dto.ScrapReasonDTO;
import com.inventory.entity.ScrapReason;
import com.inventory.service.ScrapReasonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/scrap-reason")
@RequiredArgsConstructor
public class ScrapReasonController {

    private final ScrapReasonService scrapReasonService;

    @GetMapping("/page")
    public Result<IPage<ScrapReason>> getPageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String reasonName,
            @RequestParam(required = false) String partType,
            @RequestParam(required = false) Integer status) {
        return Result.success(scrapReasonService.getPageList(pageNum, pageSize, reasonName, partType, status));
    }

    @GetMapping("/{id}")
    public Result<ScrapReason> getById(@PathVariable Long id) {
        return Result.success(scrapReasonService.getById(id));
    }

    @GetMapping("/list")
    public Result<List<ScrapReason>> listAll() {
        return Result.success(scrapReasonService.list());
    }

    @GetMapping("/enabled")
    public Result<List<ScrapReason>> listEnabled() {
        return Result.success(scrapReasonService.listEnabled());
    }

    @GetMapping("/by-part-type")
    public Result<List<ScrapReason>> listByPartType(@RequestParam String partType) {
        return Result.success(scrapReasonService.listByPartType(partType));
    }

    @PostMapping
    public Result<ScrapReason> create(@Valid @RequestBody ScrapReasonDTO dto) {
        return Result.success(scrapReasonService.create(dto));
    }

    @PutMapping
    public Result<ScrapReason> update(@Valid @RequestBody ScrapReasonDTO dto) {
        return Result.success(scrapReasonService.update(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scrapReasonService.delete(id);
        return Result.success();
    }

    @PostMapping("/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        scrapReasonService.toggleStatus(id);
        return Result.success();
    }
}
