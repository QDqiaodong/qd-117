package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DiffConfirmDTO {

    @NotNull(message = "盘点记录ID不能为空")
    private Long recordId;

    @NotBlank(message = "处理结论不能为空")
    private String handleConclusion;

    @NotBlank(message = "确认人不能为空")
    private String confirmPerson;
}
