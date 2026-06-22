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
@TableName("stock_threshold_config")
public class StockThresholdConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "零件类型不能为空")
    private String partType;

    @NotNull(message = "危险阈值不能为空")
    private Integer dangerThreshold;

    @NotNull(message = "警告阈值不能为空")
    private Integer warningThreshold;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
