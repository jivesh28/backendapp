package com.smartexpense.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Kafka message payload for async Excel upload processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseUploadMessage {
    private Long userId;
    private Long batchId;
    private int rowNumber;
    private BigDecimal amount;
    private String description;
    private LocalDate date;
}
