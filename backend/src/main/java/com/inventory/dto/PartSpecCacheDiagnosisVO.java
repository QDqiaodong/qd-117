package com.inventory.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PartSpecCacheDiagnosisVO {

    private Integer totalCacheCount;

    private Integer pinCacheCount;

    private Integer gasketCacheCount;

    private Integer totalDbCount;

    private Integer pinDbCount;

    private Integer gasketDbCount;

    private LocalDateTime cacheLastUpdateTime;

    private LocalDateTime dbLastUpdateTime;

    private List<String> missingInCache;

    private List<String> missingInDb;

    private List<SpecDiff> diffs;

    @Data
    public static class SpecDiff {
        private String partModel;
        private String partType;
        private String dbSpecParams;
        private String cacheSpecParams;
        private Integer dbStockQuantity;
        private Integer cacheStockQuantity;
        private String dbShelfNo;
        private String cacheShelfNo;
    }
}
