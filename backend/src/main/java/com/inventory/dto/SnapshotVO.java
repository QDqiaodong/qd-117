package com.inventory.dto;

import com.inventory.entity.StockCheckSnapshot;
import lombok.Data;

import java.util.List;

@Data
public class SnapshotVO {

    private String quarter;

    private Integer totalCount;

    private Integer pinCount;

    private Integer shimCount;

    private List<StockCheckSnapshot> items;
}
