package com.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("stock_check_snapshot")
public class StockCheckSnapshot implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String quarter;

    private Long partId;

    private String partModel;

    private String partName;

    private String partType;

    private Integer frozenStockQuantity;

    private String frozenShelfNo;

    private LocalDateTime createTime;
}
