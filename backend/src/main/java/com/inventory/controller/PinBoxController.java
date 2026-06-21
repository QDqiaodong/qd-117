package com.inventory.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.inventory.common.Result;
import com.inventory.entity.PinBox;
import com.inventory.service.PinBoxService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pin-box")
@RequiredArgsConstructor
public class PinBoxController {

    private final PinBoxService pinBoxService;

    @GetMapping("/page")
    public Result<IPage<PinBox>> getPageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String partModel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String boxNo,
            @RequestParam(required = false) String productionLine,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(pinBoxService.getPageList(pageNum, pageSize, partModel, status, boxNo, productionLine, startTime, endTime));
    }

    @GetMapping("/available/{partId}")
    public Result<List<PinBox>> getAvailableByPartId(@PathVariable Long partId) {
        return Result.success(pinBoxService.getAvailableByPartId(partId));
    }

    @GetMapping("/by-stock-in/{recordId}")
    public Result<List<PinBox>> getByStockInRecordId(@PathVariable Long recordId) {
        return Result.success(pinBoxService.getByStockInRecordId(recordId));
    }

    @GetMapping("/by-stock-out/{recordId}")
    public Result<List<PinBox>> getByStockOutRecordId(@PathVariable Long recordId) {
        return Result.success(pinBoxService.getByStockOutRecordId(recordId));
    }
}
