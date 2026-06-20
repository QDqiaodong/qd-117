package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class ShelfCapacityDTO implements Serializable {

    private Long id;

    @NotBlank(message = "货架编号不能为空")
    private String shelfNo;

    @NotNull(message = "顶针最大盒数不能为空")
    private Integer maxPinBoxes;

    @NotNull(message = "垫片最大包数不能为空")
    private Integer maxShimPacks;

    private String remark;
}
