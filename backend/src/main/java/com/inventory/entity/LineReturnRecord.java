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
@TableName("line_return_record")
public class LineReturnRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "小件ID不能为空")
    private Long partId;

    @NotBlank(message = "零件型号不能为空")
    private String partModel;

    @NotNull(message = "退回数量不能为空")
    private Integer quantity;

    @NotNull(message = "合格数量不能为空")
    private Integer qualifiedQuantity;

    @NotNull(message = "不合格数量不能为空")
    private Integer unqualifiedQuantity;

    @NotBlank(message = "可复用状态不能为空")
    private String reusableStatus;

    @NotBlank(message = "退回产线不能为空")
    private String productionLine;

    private String originalReceiver;

    @NotBlank(message = "操作人不能为空")
    private String operator;

    private String remark;

    private LocalDateTime createTime;
}
