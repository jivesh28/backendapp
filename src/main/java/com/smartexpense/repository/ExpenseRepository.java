package com.smartexpense.repository;

import com.smartexpense.model.Expense;
import com.smartexpense.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserOrderByDateDesc(User user);

    List<Expense> findByUserAndIsAnomalyTrue(User user);

    @Query("""
        SELECT e FROM Expense e
        WHERE e.user = :user
        AND (:categoryId IS NULL OR e.category.id = :categoryId)
        AND (:startDate IS NULL OR e.date >= :startDate)
        AND (:endDate IS NULL OR e.date <= :endDate)
        ORDER BY e.date DESC
    """)
    List<Expense> findByFilters(
        @Param("user") User user,
        @Param("categoryId") Long categoryId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT AVG(e.amount) FROM Expense e
        WHERE e.user = :user
        AND e.category.id = :categoryId
        AND e.date >= :fromDate
    """)
    BigDecimal findAverageAmountByCategoryAndUser(
        @Param("user") User user,
        @Param("categoryId") Long categoryId,
        @Param("fromDate") LocalDate fromDate
    );

    @Query("""
        SELECT e.category.name, SUM(e.amount), COUNT(e)
        FROM Expense e
        WHERE e.user = :user
        AND MONTH(e.date) = :month
        AND YEAR(e.date) = :year
        GROUP BY e.category.name
    """)
    List<Object[]> getMonthlySummary(
        @Param("user") User user,
        @Param("month") int month,
        @Param("year") int year
    );
}
