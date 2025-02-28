package com.api.expo.repository;

import com.api.expo.models.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.time.format.DateTimeFormatter;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, String> {
    // Utiliser le format String au lieu de YearMonth
    List<Budget> findByUserIdAndYearMonthStr(String userId, String yearMonthStr);
    
    Optional<Budget> findByUserIdAndCategoryIdAndYearMonthStr(String userId, String categoryId, String yearMonthStr);
    
    Optional<Budget> findByUserIdAndYearMonthStrAndCategoryIdIsNull(String userId, String yearMonthStr);
    
    // Méthodes de conversion pratiques
    default List<Budget> findByUserIdAndYearMonth(String userId, YearMonth yearMonth) {
        String yearMonthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return findByUserIdAndYearMonthStr(userId, yearMonthStr);
    }
    
    default Optional<Budget> findByUserIdAndCategoryIdAndYearMonth(String userId, String categoryId, YearMonth yearMonth) {
        String yearMonthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return findByUserIdAndCategoryIdAndYearMonthStr(userId, categoryId, yearMonthStr);
    }
    
    default Optional<Budget> findByUserIdAndYearMonthAndCategoryIdIsNull(String userId, YearMonth yearMonth) {
        String yearMonthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return findByUserIdAndYearMonthStrAndCategoryIdIsNull(userId, yearMonthStr);
    }
}