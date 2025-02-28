// ExpenseCategoryRepository.java
package com.api.expo.repository;

import com.api.expo.models.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, String> {
    List<ExpenseCategory> findByUserIdOrderByNameAsc(String userId);
}
