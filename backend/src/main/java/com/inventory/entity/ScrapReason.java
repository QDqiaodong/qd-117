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
@TableName("scrap_reason")
public class ScrapReason implements Serializable {

    @TableId(type = IdType.AUTO)
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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
