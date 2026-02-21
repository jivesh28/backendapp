package com.smartexpense.controller;

import com.smartexpense.dto.ApiResponse;
import com.smartexpense.dto.BatchDto;
import com.smartexpense.model.ExpenseBatch;
import com.smartexpense.model.User;
import com.smartexpense.repository.ExpenseBatchRepository;
import com.smartexpense.service.ExcelUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Tag(name = "Excel Upload", description = "Async Excel bulk upload via Kafka")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseUploadController {

    private final ExcelUploadService excelUploadService;
    private final ExpenseBatchRepository batchRepository;

    @PostMapping("/upload")
    @Operation(
        summary = "Upload Excel expense sheet",
        description = "Upload a .xlsx file. Rows are published to Kafka and processed asynchronously. " +
                      "Returns a batchId to track progress. Expected columns: amount | description | date (YYYY-MM-DD)"
    )
    public ResponseEntity<ApiResponse<BatchDto.BatchStatusResponse>> uploadExpenses(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Excel file (.xlsx) with columns: amount, description, date")
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is empty"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Only .xlsx files are supported"));
        }

        ExpenseBatch batch = excelUploadService.processUpload(user, file);
        return ResponseEntity.ok(ApiResponse.success(
                "File uploaded. " + batch.getTotalRows() + " rows queued for processing.",
                toBatchResponse(batch)));
    }

    @GetMapping("/batch/{batchId}")
    @Operation(
        summary = "Check batch status",
        description = "Poll the processing status of a previously uploaded Excel batch"
    )
    public ResponseEntity<ApiResponse<BatchDto.BatchStatusResponse>> getBatchStatus(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Batch ID returned from upload") @PathVariable Long batchId) {

        ExpenseBatch batch = batchRepository.findByIdAndUser(batchId, user)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found or access denied"));

        return ResponseEntity.ok(ApiResponse.success("Batch status fetched", toBatchResponse(batch)));
    }

    private BatchDto.BatchStatusResponse toBatchResponse(ExpenseBatch batch) {
        double progress = batch.getTotalRows() > 0
                ? (double) (batch.getProcessedRows() + batch.getFailedRows()) / batch.getTotalRows() * 100
                : 0;

        return BatchDto.BatchStatusResponse.builder()
                .batchId(batch.getId())
                .fileName(batch.getFileName())
                .status(batch.getStatus().name())
                .totalRows(batch.getTotalRows())
                .processedRows(batch.getProcessedRows())
                .failedRows(batch.getFailedRows())
                .progressPercent(Math.round(progress * 10.0) / 10.0)
                .createdAt(batch.getCreatedAt())
                .completedAt(batch.getCompletedAt())
                .build();
    }
}
