package com.smartexpense.service;

import com.smartexpense.model.Category;
import com.smartexpense.model.Expense;
import com.smartexpense.model.User;
import com.smartexpense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Detects anomalous expenses by comparing to the user's 3-month rolling average
 * in the same category. Flags if amount > anomaly.multiplier × average.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final ExpenseRepository expenseRepository;

    @Value("${anomaly.multiplier}")
    private double anomalyMultiplier;

    public void detectAndFlag(Expense expense) {
        User user = expense.getUser();
        Category category = expense.getCategory();

        if (category == null) return;

        LocalDate threeMonthsAgo = expense.getDate().minusMonths(3);
        BigDecimal avg = expenseRepository.findAverageAmountByCategoryAndUser(
                user, category.getId(), threeMonthsAgo);

        if (avg == null || avg.compareTo(BigDecimal.ZERO) == 0) {
            // No history, can't determine anomaly
            return;
        }

        BigDecimal threshold = avg.multiply(BigDecimal.valueOf(anomalyMultiplier));

        if (expense.getAmount().compareTo(threshold) > 0) {
            expense.setAnomaly(true);
            expense.setAnomalyReason(String.format(
                "Amount ₹%.2f is %.1fx the 3-month average ₹%.2f for category '%s'",
                expense.getAmount(), anomalyMultiplier, avg, category.getName()
            ));
            log.info("🚨 Anomaly detected: {} in {} — ₹{} > ₹{}",
                    user.getEmail(), category.getName(), expense.getAmount(), threshold);
        }
    }
}
