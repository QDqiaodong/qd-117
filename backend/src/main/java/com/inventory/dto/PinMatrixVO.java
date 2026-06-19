package com.inventory.dto;

import lombok.Data;

import java.util.List;

@Data
public class PinMatrixVO {

    private List<String> materials;

    private List<String> diameters;

    private List<String> lengths;

    private List<PinMatrixCell> cells;

    private Integer totalTypes;

    private Integer totalStock;

    private Integer skipped;

    @Data
    public static class PinMatrixCell {

        private String diameter;

        private String length;

        private String material;

        private Integer quantity;

        private String shelfNo;

        private String partModel;
    }
}
