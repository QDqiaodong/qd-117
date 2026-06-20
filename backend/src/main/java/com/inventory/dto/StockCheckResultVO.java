package com.inventory.dto;

import com.inventory.entity.StockCheckRecord;
import lombok.Data;

import java.util.List;

@Data
public class StockCheckResultVO {

    private List<StockCheckRecord> addedRecords;

    private List<StockCheckRecord> duplicateRecords;
}
