// FinanceController.java
package com.api.expo.controllers;

import com.api.expo.models.Budget;
import com.api.expo.models.Expense;
import com.api.expo.models.ExpenseCategory;
import com.api.expo.services.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class FinanceController {
    
    private final ExpenseService expenseService;
    
    // Gestion des catégories
    @GetMapping("/categories")
    public ResponseEntity<?> getUserCategories(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<ExpenseCategory> categories = expenseService.getUserCategories(userDetails);
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération des catégories");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(
            @RequestBody ExpenseCategory category,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ExpenseCategory savedCategory = expenseService.createCategory(userDetails, category);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Catégorie créée avec succès");
            response.put("category", savedCategory);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la création de la catégorie");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<?> updateCategory(
            @PathVariable String categoryId,
            @RequestBody ExpenseCategory category,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ExpenseCategory updatedCategory = expenseService.updateCategory(userDetails, categoryId, category);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Catégorie mise à jour avec succès");
            response.put("category", updatedCategory);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la mise à jour de la catégorie");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<?> deleteCategory(
            @PathVariable String categoryId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            expenseService.deleteCategory(userDetails, categoryId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Catégorie supprimée avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la suppression de la catégorie");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Gestion des dépenses
    @GetMapping("/expenses")
    public ResponseEntity<?> getUserExpenses(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<Expense> expenses = expenseService.getUserExpenses(userDetails);
            return ResponseEntity.ok(expenses);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération des dépenses");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/expenses/range")
    public ResponseEntity<?> getUserExpensesByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<Expense> expenses = expenseService.getUserExpensesInRange(userDetails, start, end);
            return ResponseEntity.ok(expenses);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération des dépenses");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/expenses")
    public ResponseEntity<?> createExpense(
            @RequestBody Expense expense,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Expense savedExpense = expenseService.createExpense(userDetails, expense);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Dépense enregistrée avec succès");
            response.put("expense", savedExpense);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de l'enregistrement de la dépense");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PutMapping("/expenses/{expenseId}")
    public ResponseEntity<?> updateExpense(
            @PathVariable String expenseId,
            @RequestBody Expense expense,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Expense updatedExpense = expenseService.updateExpense(userDetails, expenseId, expense);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Dépense mise à jour avec succès");
            response.put("expense", updatedExpense);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la mise à jour de la dépense");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<?> deleteExpense(
            @PathVariable String expenseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            expenseService.deleteExpense(userDetails, expenseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Dépense supprimée avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la suppression de la dépense");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Gestion des budgets
    @GetMapping("/budgets/{yearMonth}")
    public ResponseEntity<?> getUserBudgets(
            @PathVariable String yearMonth,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            YearMonth ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
            List<Budget> budgets = expenseService.getUserBudgets(userDetails, ym);
            return ResponseEntity.ok(budgets);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération des budgets");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/budgets")
    public ResponseEntity<?> createOrUpdateBudget(
            @RequestBody Budget budget,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Budget savedBudget = expenseService.createOrUpdateBudget(userDetails, budget);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Budget enregistré avec succès");
            response.put("budget", savedBudget);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de l'enregistrement du budget");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @DeleteMapping("/budgets/{budgetId}")
    public ResponseEntity<?> deleteBudget(
            @PathVariable String budgetId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            expenseService.deleteBudget(userDetails, budgetId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Budget supprimé avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la suppression du budget");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Résumé financier
    @GetMapping("/summary/{yearMonth}")
    public ResponseEntity<?> getMonthlyFinancialSummary(
            @PathVariable String yearMonth,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            YearMonth ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
            Map<String, Object> summary = expenseService.getMonthlyFinancialSummary(userDetails, ym);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération du résumé financier");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
