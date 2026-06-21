package com.inventory.dto;

import com.inventory.dto.StockOutDTO.StockOutItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QuotaCheckDTO implements Serializable {

    @NotBlank(message = "领用产线不能为空")
    private String productionLine;

    @NotEmpty(message = "领用明细不能为空")
    private List<StockOutItem> items;
}
