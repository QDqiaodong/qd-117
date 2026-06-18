package com.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("small_part")
public class SmallPart implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "零件型号不能为空")
    private String partModel;

    @NotBlank(message = "零件名称不能为空")
    private String partName;

    @NotBlank(message = "零件类型不能为空")
    private String partType;

    private String specParams;

    @NotBlank(message = "存放货架编号不能为空")
    private String shelfNo;

    @NotNull(message = "库存数量不能为空")
    private Integer stockQuantity;

    private String unit;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
