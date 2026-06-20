package com.inventory.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.inventory.common.Result;
import com.inventory.dto.StockCheckDTO;
import com.inventory.dto.StockCheckHotZoneVO;
import com.inventory.dto.StockCheckResultVO;
import com.inventory.entity.StockCheckRecord;
import com.inventory.service.StockCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stock-check")
@RequiredArgsConstructor
public class StockCheckController {

    private final StockCheckService stockCheckService;

    @GetMapping("/page")
    public Result<IPage<StockCheckRecord>> getPageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String partModel,
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(stockCheckService.getPageList(pageNum, pageSize, partModel, quarter, startTime, endTime));
    }

    @PostMapping
    public Result<StockCheckResultVO> checkStock(@Valid @RequestBody StockCheckDTO dto) {
        StockCheckResultVO result = stockCheckService.checkStock(dto);
        if (result.getDuplicateRecords() != null && !result.getDuplicateRecords().isEmpty()) {
            return Result.success("存在重复盘点记录", result);
        }
        return Result.success("盘点记录保存成功", result);
    }

    @GetMapping("/hot-zone")
    public Result<StockCheckHotZoneVO> getHotZone(@RequestParam(required = false) String quarter) {
        return Result.success(stockCheckService.getHotZone(quarter));
    }
}
