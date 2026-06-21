package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class ScrapReasonDTO implements Serializable {

    private Long id;

    @NotBlank(message = "原因名称不能为空")
    private String reasonName;

    @NotBlank(message = "原因编码不能为空")
    private String reasonCode;

    @NotBlank(message = "适用零件类型不能为空")
    private String partType;

    @NotNull(message = "排序号不能为空")
    private Integer sort;

    private Integer status;

    private String remark;
}
