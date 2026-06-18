package com.inventory.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.inventory.common.Result;
import com.inventory.dto.StockOutDTO;
import com.inventory.entity.StockOutRecord;
import com.inventory.service.StockOutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock-out")
@RequiredArgsConstructor
public class StockOutController {

    private final StockOutService stockOutService;

    @GetMapping("/page")
    public Result<IPage<StockOutRecord>> getPageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String partModel,
            @RequestParam(required = false) String productionLine,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(stockOutService.getPageList(pageNum, pageSize, partModel, productionLine, startTime, endTime));
    }

    @PostMapping
    public Result<List<StockOutRecord>> stockOut(@Valid @RequestBody StockOutDTO dto) {
        return Result.success("出库成功", stockOutService.stockOut(dto));
    }
}
