package com.inventory.dto;

import lombok.Data;

import java.util.List;

@Data
public class ShimMatrixVO {

    private List<String> thicknesses;

    private List<String> outerDiameters;

    private List<ShimMatrixCell> cells;

    private Integer totalTypes;

    private Integer totalStock;

    private Integer skipped;

    @Data
    public static class ShimMatrixCell {

        private String thickness;

        private String outerDiameter;

        private Integer quantity;

        private String shelfNo;

        private String partModel;
    }
}
