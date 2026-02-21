package com.smartexpense.repository;

import com.smartexpense.model.ExpenseBatch;
import com.smartexpense.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseBatchRepository extends JpaRepository<ExpenseBatch, Long> {
    Optional<ExpenseBatch> findByIdAndUser(Long id, User user);
    List<ExpenseBatch> findByUserOrderByCreatedAtDesc(User user);
}
