package com.inventory.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class QuotaCheckResultVO implements Serializable {

    private Boolean passed;

    private String quarter;

    private String productionLine;

    private List<QuotaDetail> details = new ArrayList<>();

    private String message;

    @Data
    public static class QuotaDetail implements Serializable {

        private String partType;

        private Boolean configured;

        private Integer maxQuantity;

        private Integer usedQuantity;

        private Integer remainingQuantity;

        private Integer requestedQuantity;

        private Integer exceededQuantity;

        private Boolean exceeded;
    }
}
