package com.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ScrapDTO {

    @NotNull(message = "报废明细不能为空")
    @NotEmpty(message = "报废明细不能为空")
    private List<ScrapItem> items;

    @NotBlank(message = "操作人不能为空")
    private String operator;

    private String remark;

    @Data
    public static class ScrapItem {

        @NotNull(message = "小件ID不能为空")
        private Long partId;

        @NotNull(message = "报废数量不能为空")
        @Min(value = 1, message = "报废数量必须大于0")
        private Integer quantity;

        @NotBlank(message = "报废原因不能为空")
        private String scrapReason;
    }
}
