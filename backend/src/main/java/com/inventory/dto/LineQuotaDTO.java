package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class LineQuotaDTO implements Serializable {

    private Long id;

    @NotBlank(message = "季度不能为空")
    private String quarter;

    @NotBlank(message = "领用产线不能为空")
    private String productionLine;

    @NotBlank(message = "小件类型不能为空")
    private String partType;

    @NotNull(message = "配额上限不能为空")
    private Integer maxQuantity;

    private String remark;
}
