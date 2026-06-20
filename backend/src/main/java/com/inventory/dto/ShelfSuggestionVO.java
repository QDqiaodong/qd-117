package com.inventory.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ShelfSuggestionVO implements Serializable {

    private String shelfNo;

    private Integer remainingCapacity;

    private Integer maxCapacity;

    private String partType;

    private String remark;
}
