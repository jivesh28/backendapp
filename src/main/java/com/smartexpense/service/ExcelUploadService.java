package com.smartexpense.service;

import com.smartexpense.kafka.ExpenseKafkaProducer;
import com.smartexpense.kafka.ExpenseUploadMessage;
import com.smartexpense.model.ExpenseBatch;
import com.smartexpense.model.User;
import com.smartexpense.repository.ExpenseBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelUploadService {

    private final ExpenseBatchRepository batchRepository;
    private final ExpenseKafkaProducer kafkaProducer;

    /**
     * Parses the Excel file, creates a batch record, and publishes each row to Kafka.
     * Expected columns: amount | description | date (YYYY-MM-DD or Date cell)
     */
    public ExpenseBatch processUpload(User user, MultipartFile file) {
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            List<ExpenseUploadMessage> messages = new ArrayList<>();

            // Skip header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    BigDecimal amount = BigDecimal.valueOf(getNumericValue(row.getCell(0)));
                    String description = getStringValue(row.getCell(1));
                    LocalDate date = getDateValue(row.getCell(2));

                    if (description == null || description.isBlank()) continue;

                    messages.add(ExpenseUploadMessage.builder()
                            .userId(user.getId())
                            .rowNumber(i)
                            .amount(amount)
                            .description(description)
                            .date(date != null ? date : LocalDate.now())
                            .build());
                } catch (Exception e) {
                    log.warn("Skipping row {}: {}", i, e.getMessage());
                }
            }

            if (messages.isEmpty()) {
                throw new IllegalArgumentException("No valid data rows found in the Excel file.");
            }

            // Create batch record
            ExpenseBatch batch = ExpenseBatch.builder()
                    .user(user)
                    .fileName(file.getOriginalFilename())
                    .totalRows(messages.size())
                    .processedRows(0)
                    .failedRows(0)
                    .build();
            batch = batchRepository.save(batch);

            // Inject batchId into messages and publish to Kafka
            final Long batchId = batch.getId();
            for (ExpenseUploadMessage msg : messages) {
                msg.setBatchId(batchId);
                kafkaProducer.publish(msg);
            }

            log.info("📁 Batch {} created with {} rows from file '{}'",
                    batchId, messages.size(), file.getOriginalFilename());
            return batch;

        } catch (Exception e) {
            log.error("Failed to process Excel upload: {}", e.getMessage());
            throw new RuntimeException("Failed to process Excel file: " + e.getMessage());
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private double getNumericValue(Cell cell) {
        if (cell == null) throw new IllegalArgumentException("Amount cell is empty");
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> Double.parseDouble(cell.getStringCellValue().trim());
            default -> throw new IllegalArgumentException("Invalid amount value");
        };
    }

    private String getStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }

    private LocalDate getDateValue(Cell cell) {
        if (cell == null) return LocalDate.now();
        return switch (cell.getCellType()) {
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate();
                }
                yield LocalDate.now();
            }
            case STRING -> {
                try {
                    yield LocalDate.parse(cell.getStringCellValue().trim());
                } catch (Exception e) {
                    yield LocalDate.now();
                }
            }
            default -> LocalDate.now();
        };
    }
}
