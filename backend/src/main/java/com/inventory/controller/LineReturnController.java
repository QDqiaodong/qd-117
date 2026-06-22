package com.inventory.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.inventory.common.Result;
import com.inventory.dto.LineReturnDTO;
import com.inventory.entity.LineReturnRecord;
import com.inventory.service.LineReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/line-return")
@RequiredArgsConstructor
public class LineReturnController {

    private final LineReturnService lineReturnService;

    @GetMapping("/page")
    public Result<IPage<LineReturnRecord>> getPageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String partModel,
            @RequestParam(required = false) String productionLine,
            @RequestParam(required = false) String reusableStatus,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(lineReturnService.getPageList(pageNum, pageSize, partModel, productionLine, reusableStatus, startTime, endTime));
    }

    @PostMapping
    public Result<List<LineReturnRecord>> lineReturn(@Valid @RequestBody LineReturnDTO dto) {
        return Result.success("退回登记成功", lineReturnService.lineReturn(dto));
    }
}
