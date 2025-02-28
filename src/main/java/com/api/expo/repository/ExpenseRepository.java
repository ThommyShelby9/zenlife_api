// ExpenseRepository.java
package com.api.expo.repository;

import com.api.expo.models.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, String> {
    List<Expense> findByUserIdOrderByDateDesc(String userId);
    List<Expense> findByUserIdAndDateBetweenOrderByDateDesc(String userId, LocalDate startDate, LocalDate endDate);
    List<Expense> findByUserIdAndCategoryIdOrderByDateDesc(String userId, String categoryId);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = ?1 AND e.date BETWEEN ?2 AND ?3")
    BigDecimal getTotalExpensesForUserInRange(String userId, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = ?1 AND e.category.id = ?2 AND e.date BETWEEN ?3 AND ?4")
    BigDecimal getTotalExpensesForUserAndCategoryInRange(String userId, String categoryId, LocalDate startDate, LocalDate endDate);
}