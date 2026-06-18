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
@TableName("stock_in_record")
public class StockInRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "小件ID不能为空")
    private Long partId;

    @NotBlank(message = "零件型号不能为空")
    private String partModel;

    @NotNull(message = "入库数量不能为空")
    private Integer quantity;

    @NotBlank(message = "存放货架编号不能为空")
    private String shelfNo;

    @NotBlank(message = "操作人不能为空")
    private String operator;

    private String remark;

    private LocalDateTime createTime;
}
