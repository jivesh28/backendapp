package com.smartexpense.service;

import com.smartexpense.dto.ExpenseDto;
import com.smartexpense.model.Category;
import com.smartexpense.model.Expense;
import com.smartexpense.model.User;
import com.smartexpense.repository.CategoryRepository;
import com.smartexpense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final CategorizationFacade categorizationFacade;
    private final AnomalyDetectionService anomalyDetectionService;

    @Transactional
    public ExpenseDto.ExpenseResponse addExpense(User user, ExpenseDto.CreateRequest request) {
        // Categorize
        Category category = categorizationFacade.categorize(request.getDescription());

        // Build expense
        Expense expense = Expense.builder()
                .user(user)
                .amount(request.getAmount())
                .description(request.getDescription())
                .category(category)
                .date(request.getDate())
                .isAnomaly(false)
                .build();

        // Detect anomaly (in-memory mutation before save)
        anomalyDetectionService.detectAndFlag(expense);

        expense = expenseRepository.save(expense);
        return toResponse(expense);
    }

    public List<ExpenseDto.ExpenseResponse> getExpenses(User user, Long categoryId,
                                                         LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByFilters(user, categoryId, startDate, endDate)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ExpenseDto.ExpenseResponse> getAnomalies(User user) {
        return expenseRepository.findByUserAndIsAnomalyTrue(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ExpenseDto.MonthlySummaryResponse getMonthlySummary(User user, int month, int year) {
        List<Object[]> rows = expenseRepository.getMonthlySummary(user, month, year);

        List<ExpenseDto.CategorySummary> breakdown = rows.stream()
                .map(row -> ExpenseDto.CategorySummary.builder()
                        .category((String) row[0])
                        .totalAmount((BigDecimal) row[1])
                        .transactionCount((Long) row[2])
                        .build())
                .collect(Collectors.toList());

        BigDecimal grandTotal = breakdown.stream()
                .map(ExpenseDto.CategorySummary::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ExpenseDto.MonthlySummaryResponse.builder()
                .month(month)
                .year(year)
                .grandTotal(grandTotal)
                .breakdown(breakdown)
                .build();
    }

    @Transactional
    public ExpenseDto.ExpenseResponse updateCategory(User user, Long expenseId, Long categoryId) {
        Expense expense = expenseRepository.findById(expenseId)
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Expense not found or access denied"));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));

        expense.setCategory(category);
        // Re-run anomaly detection after category change
        expense.setAnomaly(false);
        expense.setAnomalyReason(null);
        anomalyDetectionService.detectAndFlag(expense);

        expense = expenseRepository.save(expense);
        return toResponse(expense);
    }

    private ExpenseDto.ExpenseResponse toResponse(Expense expense) {
        return ExpenseDto.ExpenseResponse.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .description(expense.getDescription())
                .category(expense.getCategory() != null ? expense.getCategory().getName() : "Uncategorized")
                .categoryId(expense.getCategory() != null ? expense.getCategory().getId() : null)
                .date(expense.getDate())
                .isAnomaly(expense.isAnomaly())
                .anomalyReason(expense.getAnomalyReason())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
