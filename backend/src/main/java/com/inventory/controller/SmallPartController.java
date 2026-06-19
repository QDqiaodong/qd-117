package com.inventory.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.inventory.cache.PartSpecCache;
import com.inventory.common.Result;
import com.inventory.dto.PartSpecCacheDiagnosisVO;
import com.inventory.dto.PinMatrixVO;
import com.inventory.entity.SmallPart;
import com.inventory.service.SmallPartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/parts")
@RequiredArgsConstructor
public class SmallPartController {

    private final SmallPartService smallPartService;
    private final PartSpecCache partSpecCache;

    @GetMapping("/page")
    public Result<IPage<SmallPart>> getPageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String partType,
            @RequestParam(required = false) String keyword) {
        return Result.success(smallPartService.getPageList(pageNum, pageSize, partType, keyword));
    }

    @GetMapping("/{id}")
    public Result<SmallPart> getById(@PathVariable Long id) {
        return Result.success(smallPartService.getById(id));
    }

    @GetMapping("/list")
    public Result<List<SmallPart>> listAll() {
        return Result.success(smallPartService.listAll());
    }

    @GetMapping("/specs")
    public Result<List<PartSpecCache.PartSpecInfo>> getSpecs(
            @RequestParam(required = false) String partType) {
        if (partType != null && !partType.isEmpty()) {
            return Result.success(partSpecCache.getSpecsByType(partType));
        }
        return Result.success(partSpecCache.getAllSpecs());
    }

    @GetMapping("/pin-matrix")
    public Result<PinMatrixVO> getPinMatrix() {
        return Result.success(smallPartService.getPinMatrix());
    }

    @PostMapping
    public Result<SmallPart> create(@Valid @RequestBody SmallPart part) {
        return Result.success(smallPartService.create(part));
    }

    @PutMapping
    public Result<SmallPart> update(@Valid @RequestBody SmallPart part) {
        return Result.success(smallPartService.update(part));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        smallPartService.delete(id);
        return Result.success();
    }

    @PostMapping("/refresh-cache")
    public Result<Void> refreshCache() {
        smallPartService.refreshCache();
        return Result.success();
    }

    @GetMapping("/cache-diagnosis")
    public Result<PartSpecCacheDiagnosisVO> getCacheDiagnosis() {
        return Result.success(smallPartService.diagnoseCache());
    }
}
