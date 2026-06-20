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
@TableName("stock_check_record")
public class StockCheckRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "小件ID不能为空")
    private Long partId;

    @NotBlank(message = "零件型号不能为空")
    private String partModel;

    @NotNull(message = "系统库存数量不能为空")
    private Integer systemQuantity;

    @NotNull(message = "实际库存数量不能为空")
    private Integer actualQuantity;

    @NotNull(message = "差异数量不能为空")
    private Integer diffQuantity;

    @NotBlank(message = "货架编号不能为空")
    private String shelfNo;

    @NotBlank(message = "盘点人不能为空")
    private String checkPerson;

    private String remark;

    @NotBlank(message = "盘点季度不能为空")
    private String quarter;

    private Long snapshotId;

    private Integer confirmStatus;

    private String handleConclusion;

    private String confirmPerson;

    private LocalDateTime confirmTime;

    private LocalDateTime createTime;
}
