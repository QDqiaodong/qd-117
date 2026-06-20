package com.inventory.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.inventory.common.Result;
import com.inventory.dto.StockInDTO;
import com.inventory.dto.StockInValidationVO;
import com.inventory.entity.StockInRecord;
import com.inventory.service.StockInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock-in")
@RequiredArgsConstructor
public class StockInController {

    private final StockInService stockInService;

    @GetMapping("/page")
    public Result<IPage<StockInRecord>> getPageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String partModel,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(stockInService.getPageList(pageNum, pageSize, partModel, startTime, endTime));
    }

    @PostMapping("/validate")
    public Result<StockInValidationVO> validate(@Valid @RequestBody StockInDTO dto) {
        return Result.success(stockInService.validate(dto));
    }

    @PostMapping
    public Result<List<StockInRecord>> stockIn(@Valid @RequestBody StockInDTO dto) {
        return Result.success("入库成功", stockInService.stockIn(dto));
    }
}
