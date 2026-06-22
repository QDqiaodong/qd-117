package com.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class LineReturnDTO {

    @NotNull(message = "退回明细不能为空")
    @NotEmpty(message = "退回明细不能为空")
    private List<LineReturnItem> items;

    @NotBlank(message = "退回产线不能为空")
    private String productionLine;

    private String originalReceiver;

    @NotBlank(message = "操作人不能为空")
    private String operator;

    private String remark;

    @Data
    public static class LineReturnItem {

        @NotNull(message = "小件ID不能为空")
        private Long partId;

        @NotNull(message = "退回数量不能为空")
        @Min(value = 1, message = "退回数量必须大于0")
        private Integer quantity;

        @NotNull(message = "合格数量不能为空")
        @Min(value = 0, message = "合格数量不能为负数")
        private Integer qualifiedQuantity;

        @NotNull(message = "不合格数量不能为空")
        @Min(value = 0, message = "不合格数量不能为负数")
        private Integer unqualifiedQuantity;

        @NotBlank(message = "可复用状态不能为空")
        private String reusableStatus;
    }
}
