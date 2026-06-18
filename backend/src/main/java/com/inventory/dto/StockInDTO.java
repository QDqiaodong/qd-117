package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class StockInDTO {

    @NotNull(message = "入库明细不能为空")
    private List<StockInItem> items;

    @NotBlank(message = "操作人不能为空")
    private String operator;

    @Data
    public static class StockInItem {

        @NotBlank(message = "零件型号不能为空")
        private String partModel;

        private String partName;

        @NotBlank(message = "零件类型不能为空")
        private String partType;

        private String specParams;

        @NotBlank(message = "存放货架编号不能为空")
        private String shelfNo;

        @NotNull(message = "入库数量不能为空")
        private Integer quantity;

        private String unit;

        private String remark;
    }
}
