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
@TableName("line_quota")
public class LineQuota implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "季度不能为空")
    private String quarter;

    @NotBlank(message = "领用产线不能为空")
    private String productionLine;

    @NotBlank(message = "小件类型不能为空")
    private String partType;

    @NotNull(message = "配额上限不能为空")
    private Integer maxQuantity;

    private Integer usedQuantity;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public Integer getRemainingQuantity() {
        int max = maxQuantity == null ? 0 : maxQuantity;
        int used = usedQuantity == null ? 0 : usedQuantity;
        return Math.max(0, max - used);
    }
}
