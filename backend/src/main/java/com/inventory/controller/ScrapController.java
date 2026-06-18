package com.inventory.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.inventory.common.Result;
import com.inventory.dto.ScrapDTO;
import com.inventory.entity.ScrapRecord;
import com.inventory.service.ScrapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/scrap")
@RequiredArgsConstructor
public class ScrapController {

    private final ScrapService scrapService;

    @GetMapping("/page")
    public Result<IPage<ScrapRecord>> getPageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String partModel,
            @RequestParam(required = false) String scrapReason,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(scrapService.getPageList(pageNum, pageSize, partModel, scrapReason, startTime, endTime));
    }

    @PostMapping
    public Result<List<ScrapRecord>> scrap(@Valid @RequestBody ScrapDTO dto) {
        return Result.success("报废登记成功", scrapService.scrap(dto));
    }
}
