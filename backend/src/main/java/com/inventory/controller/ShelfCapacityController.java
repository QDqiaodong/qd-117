package com.inventory.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.inventory.common.Result;
import com.inventory.dto.ShelfCapacityDTO;
import com.inventory.dto.ShelfSuggestionVO;
import com.inventory.dto.StockInDTO;
import com.inventory.dto.StockInValidationVO;
import com.inventory.entity.ShelfCapacity;
import com.inventory.service.ShelfCapacityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shelf-capacity")
@RequiredArgsConstructor
public class ShelfCapacityController {

    private final ShelfCapacityService shelfCapacityService;

    @GetMapping("/page")
    public Result<IPage<ShelfCapacity>> getPageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String shelfNo) {
        return Result.success(shelfCapacityService.getPageList(pageNum, pageSize, shelfNo));
    }

    @GetMapping("/{id}")
    public Result<ShelfCapacity> getById(@PathVariable Long id) {
        return Result.success(shelfCapacityService.getById(id));
    }

    @GetMapping("/by-shelf/{shelfNo}")
    public Result<ShelfCapacity> getByShelfNo(@PathVariable String shelfNo) {
        return Result.success(shelfCapacityService.getByShelfNo(shelfNo));
    }

    @GetMapping("/list")
    public Result<List<ShelfCapacity>> listAll() {
        return Result.success(shelfCapacityService.list());
    }

    @GetMapping("/available-shelves")
    public Result<List<ShelfSuggestionVO>> getAvailableShelves(
            @RequestParam String partType,
            @RequestParam(defaultValue = "1") Integer requiredQuantity) {
        return Result.success(shelfCapacityService.getAvailableShelves(partType, requiredQuantity));
    }

    @PostMapping("/validate-stock-in")
    public Result<StockInValidationVO> validateStockIn(@Valid @RequestBody StockInDTO dto) {
        return Result.success(shelfCapacityService.validateStockIn(dto));
    }

    @PostMapping
    public Result<ShelfCapacity> create(@Valid @RequestBody ShelfCapacityDTO dto) {
        return Result.success(shelfCapacityService.create(dto));
    }

    @PutMapping
    public Result<ShelfCapacity> update(@Valid @RequestBody ShelfCapacityDTO dto) {
        return Result.success(shelfCapacityService.update(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        shelfCapacityService.delete(id);
        return Result.success();
    }

    @PostMapping("/recalculate/{shelfNo}")
    public Result<Void> recalculateCurrentUsage(@PathVariable String shelfNo) {
        shelfCapacityService.recalculateCurrentUsage(shelfNo);
        return Result.success();
    }
}
