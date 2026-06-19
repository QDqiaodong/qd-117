package com.inventory.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StockCheckHotZoneVO {

    private List<ShelfHotZone> shelves;

    private Integer totalShelves;

    private Integer totalGain;

    private Integer totalLoss;

    private Integer totalRows;

    @Data
    public static class ShelfHotZone {

        private String shelfNo;

        private Integer pinGain;

        private Integer pinLoss;

        private Integer gasketGain;

        private Integer gasketLoss;

        private Integer totalGain;

        private Integer totalLoss;

        private Integer rowCount;

        private List<HotZoneRow> rows;
    }

    @Data
    public static class HotZoneRow {

        private String shelfNo;

        private String partModel;

        private String partType;

        private Integer systemQuantity;

        private Integer actualQuantity;

        private Integer diffQuantity;

        private String checkPerson;

        private String remark;

        private String quarter;

        private LocalDateTime createTime;
    }
}
