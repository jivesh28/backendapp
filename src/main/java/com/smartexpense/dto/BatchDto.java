package com.smartexpense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class BatchDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchStatusResponse {
        private Long batchId;
        private String fileName;
        private String status;
        private int totalRows;
        private int processedRows;
        private int failedRows;
        private double progressPercent;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
    }
}
