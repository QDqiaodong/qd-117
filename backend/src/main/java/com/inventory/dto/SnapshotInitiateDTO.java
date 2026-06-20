package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SnapshotInitiateDTO {

    @NotBlank(message = "盘点季度不能为空")
    private String quarter;

    private String operator;
}
