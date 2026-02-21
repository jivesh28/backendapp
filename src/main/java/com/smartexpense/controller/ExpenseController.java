package com.smartexpense.controller;

import com.smartexpense.dto.ApiResponse;
import com.smartexpense.dto.ExpenseDto;
import com.smartexpense.model.User;
import com.smartexpense.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Core expense management — add, list, summarize, and detect anomalies")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @Operation(
        summary = "Add a new expense",
        description = "Adds an expense, auto-categorizes it (keyword → Gemini LLM fallback), and flags anomalies if amount > 3× the category average"
    )
    public ResponseEntity<ApiResponse<ExpenseDto.ExpenseResponse>> addExpense(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ExpenseDto.CreateRequest request) {
        ExpenseDto.ExpenseResponse response = expenseService.addExpense(user, request);
        return ResponseEntity.ok(ApiResponse.success("Expense added successfully", response));
    }

    @GetMapping
    @Operation(
        summary = "List expenses",
        description = "Returns all expenses for the authenticated user. Optionally filter by category ID, start date, and end date."
    )
    public ResponseEntity<ApiResponse<List<ExpenseDto.ExpenseResponse>>> getExpenses(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Start date (YYYY-MM-DD)") @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)") @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<ExpenseDto.ExpenseResponse> expenses = expenseService.getExpenses(user, categoryId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Expenses fetched", expenses));
    }

    @GetMapping("/summary")
    @Operation(
        summary = "Monthly expense summary",
        description = "Returns total spending grouped by category for a specific month and year"
    )
    public ResponseEntity<ApiResponse<ExpenseDto.MonthlySummaryResponse>> getSummary(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Month (1-12)", required = true) @RequestParam int month,
            @Parameter(description = "Year (e.g. 2026)", required = true) @RequestParam int year) {
        ExpenseDto.MonthlySummaryResponse summary = expenseService.getMonthlySummary(user, month, year);
        return ResponseEntity.ok(ApiResponse.success("Summary fetched", summary));
    }

    @GetMapping("/anomalies")
    @Operation(
        summary = "Get flagged anomalous expenses",
        description = "Returns all expenses flagged as unusual (amount exceeded 3× the 3-month category average)"
    )
    public ResponseEntity<ApiResponse<List<ExpenseDto.ExpenseResponse>>> getAnomalies(
            @AuthenticationPrincipal User user) {
        List<ExpenseDto.ExpenseResponse> anomalies = expenseService.getAnomalies(user);
        return ResponseEntity.ok(ApiResponse.success("Anomalies fetched", anomalies));
    }

    @PutMapping("/{id}/category")
    @Operation(
        summary = "Override expense category",
        description = "Manually reassign the category of an expense. Anomaly detection re-runs after override."
    )
    public ResponseEntity<ApiResponse<ExpenseDto.ExpenseResponse>> updateCategory(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Expense ID") @PathVariable Long id,
            @Valid @RequestBody ExpenseDto.UpdateCategoryRequest request) {
        ExpenseDto.ExpenseResponse response = expenseService.updateCategory(user, id, request.getCategoryId());
        return ResponseEntity.ok(ApiResponse.success("Category updated", response));
    }
}
