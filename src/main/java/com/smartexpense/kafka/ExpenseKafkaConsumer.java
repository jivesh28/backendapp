package com.smartexpense.kafka;

import com.smartexpense.dto.ExpenseDto;
import com.smartexpense.model.ExpenseBatch;
import com.smartexpense.model.User;
import com.smartexpense.repository.ExpenseBatchRepository;
import com.smartexpense.repository.UserRepository;
import com.smartexpense.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseKafkaConsumer {

    private final ExpenseService expenseService;
    private final UserRepository userRepository;
    private final ExpenseBatchRepository batchRepository;

    @KafkaListener(topics = "${kafka.topics.expense-upload}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consume(ExpenseUploadMessage message) {
        log.info("📥 Processing row {} for batch {}", message.getRowNumber(), message.getBatchId());

        ExpenseBatch batch = batchRepository.findById(message.getBatchId()).orElse(null);
        if (batch == null) {
            log.error("Batch {} not found", message.getBatchId());
            return;
        }

        // Update batch to PROCESSING on first row
        if (batch.getStatus() == ExpenseBatch.BatchStatus.PENDING) {
            batch.setStatus(ExpenseBatch.BatchStatus.PROCESSING);
            batchRepository.save(batch);
        }

        try {
            User user = userRepository.findById(message.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + message.getUserId()));

            ExpenseDto.CreateRequest request = ExpenseDto.CreateRequest.builder()
                    .amount(message.getAmount())
                    .description(message.getDescription())
                    .date(message.getDate())
                    .build();

            expenseService.addExpense(user, request);

            batch.setProcessedRows(batch.getProcessedRows() + 1);
            log.info("✅ Row {} processed successfully for batch {}", message.getRowNumber(), message.getBatchId());

        } catch (Exception e) {
            log.error("❌ Failed to process row {} for batch {}: {}", message.getRowNumber(), message.getBatchId(), e.getMessage());
            batch.setFailedRows(batch.getFailedRows() + 1);
        }

        // Check if batch is complete
        int processed = batch.getProcessedRows() + batch.getFailedRows();
        if (processed >= batch.getTotalRows()) {
            batch.setStatus(batch.getFailedRows() > 0 && batch.getProcessedRows() == 0
                    ? ExpenseBatch.BatchStatus.FAILED
                    : ExpenseBatch.BatchStatus.COMPLETED);
            batch.setCompletedAt(LocalDateTime.now());
            log.info("🏁 Batch {} completed: {}/{} processed, {} failed",
                    batch.getId(), batch.getProcessedRows(), batch.getTotalRows(), batch.getFailedRows());
        }

        batchRepository.save(batch);
    }
}
