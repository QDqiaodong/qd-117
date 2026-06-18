package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class StockCheckDTO {

    @NotNull(message = "盘点明细不能为空")
    private List<StockCheckItem> items;

    @NotBlank(message = "盘点季度不能为空")
    private String quarter;

    @NotBlank(message = "盘点人不能为空")
    private String checkPerson;

    @Data
    public static class StockCheckItem {

        @NotNull(message = "小件ID不能为空")
        private Long partId;

        @NotNull(message = "实际库存数量不能为空")
        private Integer actualQuantity;

        private String remark;
    }
}
