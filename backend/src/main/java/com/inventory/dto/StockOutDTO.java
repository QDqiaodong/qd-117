package com.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class StockOutDTO {

    @NotNull(message = "出库明细不能为空")
    @NotEmpty(message = "出库明细不能为空")
    private List<StockOutItem> items;

    @NotBlank(message = "领用产线不能为空")
    private String productionLine;

    @NotBlank(message = "操作人不能为空")
    private String operator;

    private String receiver;

    private String remark;

    @Data
    public static class StockOutItem {

        @NotNull(message = "小件ID不能为空")
        private Long partId;

        @NotNull(message = "领用数量不能为空")
        @Min(value = 1, message = "领用数量必须大于0")
        private Integer quantity;
    }
}
