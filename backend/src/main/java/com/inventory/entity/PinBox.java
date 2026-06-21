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
@TableName("pin_box")
public class PinBox implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "盒号不能为空")
    private String boxNo;

    @NotNull(message = "零件ID不能为空")
    private Long partId;

    @NotBlank(message = "零件型号不能为空")
    private String partModel;

    @NotBlank(message = "盒号状态不能为空")
    private String status;

    private Long stockInRecordId;

    private Long stockOutRecordId;

    private String productionLine;

    @NotBlank(message = "货架编号不能为空")
    private String shelfNo;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
