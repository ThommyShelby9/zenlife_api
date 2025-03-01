// ExpenseService.java
package com.api.expo.services;

import com.api.expo.models.Budget;
import com.api.expo.models.Expense;
import com.api.expo.models.ExpenseCategory;
import com.api.expo.models.User;
import com.api.expo.repository.BudgetRepository;
import com.api.expo.repository.ExpenseCategoryRepository;
import com.api.expo.repository.ExpenseRepository;
import com.api.expo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    
    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
 * Récupère les dépenses les plus récentes d'un utilisateur
 * @param userDetails Utilisateur authentifié
 * @param limit Nombre maximum de dépenses à retourner
 * @return Liste des dépenses récentes
 */
    public List<Expense> getRecentExpenses(UserDetails userDetails, int limit) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        // Récupérer les dépenses triées par date décroissante et limitées
        return expenseRepository.findByUserIdOrderByDateDesc(user.getId())
            .stream()
            .limit(limit)
            .toList();
    }
    
    // Gestion des catégories
    public List<ExpenseCategory> getUserCategories() {

            
        return expenseCategoryRepository.findAllByOrderByNameAsc();
    }
    
    public ExpenseCategory createCategory(UserDetails userDetails, ExpenseCategory category) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        category.setCreatedAt(Instant.now());
        category.setUpdatedAt(Instant.now());
        
        return expenseCategoryRepository.save(category);
    }
    
    public ExpenseCategory updateCategory(UserDetails userDetails, String categoryId, ExpenseCategory updatedCategory) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        ExpenseCategory category = expenseCategoryRepository.findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
            

        
        category.setName(updatedCategory.getName());
        category.setIcon(updatedCategory.getIcon());
        category.setColor(updatedCategory.getColor());
        category.setUpdatedAt(Instant.now());
        
        return expenseCategoryRepository.save(category);
    }
    
    public void deleteCategory(UserDetails userDetails, String categoryId) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        ExpenseCategory category = expenseCategoryRepository.findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
            

        
        // Vérifier s'il y a des dépenses associées à cette catégorie
        List<Expense> expenses = expenseRepository.findByUserIdAndCategoryIdOrderByDateDesc(user.getId(), categoryId);
        if (!expenses.isEmpty()) {
            throw new RuntimeException("Impossible de supprimer cette catégorie car elle est utilisée par des dépenses");
        }
        
        expenseCategoryRepository.delete(category);
    }
    
    // Gestion des dépenses
    public List<Expense> getUserExpenses(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        return expenseRepository.findByUserIdOrderByDateDesc(user.getId());
    }
    
    public List<Expense> getUserExpensesInRange(UserDetails userDetails, LocalDate start, LocalDate end) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        return expenseRepository.findByUserIdAndDateBetweenOrderByDateDesc(user.getId(), start, end);
    }
    
    public Expense createExpense(UserDetails userDetails, Expense expense) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        // Vérifier que la catégorie appartient à l'utilisateur
        ExpenseCategory category = expenseCategoryRepository.findById(expense.getCategory().getId())
            .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
            

        
        expense.setUser(user);
        expense.setCategory(category);
        expense.setCreatedAt(Instant.now());
        expense.setUpdatedAt(Instant.now());
        
        Expense savedExpense = expenseRepository.save(expense);
        
        // Vérifier si cette dépense dépasse le budget
        checkBudgetAlert(user, category, expense.getDate(), expense.getAmount());
        
        return savedExpense;
    }
    
    public Expense updateExpense(UserDetails userDetails, String expenseId, Expense updatedExpense) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new RuntimeException("Dépense non trouvée"));
            
        if (!expense.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à modifier cette dépense");
        }
        
        // Vérifier que la catégorie appartient à l'utilisateur
        ExpenseCategory category = expenseCategoryRepository.findById(updatedExpense.getCategory().getId())
            .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
            
        
        expense.setCategory(category);
        expense.setAmount(updatedExpense.getAmount());
        expense.setTitle(updatedExpense.getTitle()); // Utilisez le getter/setter généré par Lombok
        expense.setDate(updatedExpense.getDate());
        expense.setDescription(updatedExpense.getDescription());
        expense.setUpdatedAt(Instant.now());
        
        return expenseRepository.save(expense);
    }
    
    public void deleteExpense(UserDetails userDetails, String expenseId) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new RuntimeException("Dépense non trouvée"));
            
        if (!expense.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer cette dépense");
        }
        
        expenseRepository.delete(expense);
    }
    
    // Gestion des budgets
    public List<Budget> getUserBudgets(UserDetails userDetails, YearMonth yearMonth) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        return budgetRepository.findByUserIdAndYearMonth(user.getId(), yearMonth);
    }
    
    public Budget createOrUpdateBudget(UserDetails userDetails, Budget budget) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        // Vérifier si c'est un budget global ou un budget de catégorie
        boolean isGlobalBudget = budget.getCategory() == null || budget.getCategory().getId() == null;
        Optional<Budget> existingBudget;
        
        if (!isGlobalBudget) {
            // C'est un budget par catégorie
            existingBudget = budgetRepository.findByUserIdAndCategoryIdAndYearMonth(
                user.getId(), budget.getCategory().getId(), budget.getYearMonth()
            );
            
            // Vérifier que la catégorie appartient à l'utilisateur
            ExpenseCategory category = expenseCategoryRepository.findById(budget.getCategory().getId())
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
                
            
            // Vérifier si le budget global existe et si le budget de catégorie ne le dépasse pas
            Optional<Budget> globalBudget = budgetRepository.findByUserIdAndYearMonthAndCategoryIdIsNull(
                user.getId(), budget.getYearMonth()
            );
            
            if (globalBudget.isPresent()) {
                // Calculer la somme des budgets de catégorie existants (excluant celui en cours de modification)
                List<Budget> categoryBudgets = budgetRepository.findByUserIdAndYearMonth(user.getId(), budget.getYearMonth())
                    .stream()
                    .filter(b -> b.getCategory() != null && !b.getId().equals(budget.getId()))
                    .collect(Collectors.toList());
                    
                BigDecimal totalCategoryBudgets = categoryBudgets.stream()
                    .map(Budget::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                // Ajouter le montant du budget en cours de création/modification
                totalCategoryBudgets = totalCategoryBudgets.add(budget.getAmount());
                
                // Vérifier si le total des budgets de catégorie dépasse le budget global
                if (totalCategoryBudgets.compareTo(globalBudget.get().getAmount()) > 0) {
                    throw new RuntimeException("Le total des sous-budgets ne peut pas dépasser le budget global");
                }
            }
        } else {
            // C'est un budget global
            existingBudget = budgetRepository.findByUserIdAndYearMonthAndCategoryIdIsNull(
                user.getId(), budget.getYearMonth()
            );
            
            // Si ce budget global est inférieur à la somme des budgets de catégorie existants, on refuse
            if (existingBudget.isEmpty()) {
                List<Budget> categoryBudgets = budgetRepository.findByUserIdAndYearMonth(user.getId(), budget.getYearMonth())
                    .stream()
                    .filter(b -> b.getCategory() != null)
                    .collect(Collectors.toList());
                    
                if (!categoryBudgets.isEmpty()) {
                    BigDecimal totalCategoryBudgets = categoryBudgets.stream()
                        .map(Budget::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                        
                    if (budget.getAmount().compareTo(totalCategoryBudgets) < 0) {
                        throw new RuntimeException("Le budget global ne peut pas être inférieur à la somme des sous-budgets existants");
                    }
                }
            } else {
                // Si on modifie un budget global existant
                List<Budget> categoryBudgets = budgetRepository.findByUserIdAndYearMonth(user.getId(), budget.getYearMonth())
                    .stream()
                    .filter(b -> b.getCategory() != null)
                    .collect(Collectors.toList());
                    
                if (!categoryBudgets.isEmpty()) {
                    BigDecimal totalCategoryBudgets = categoryBudgets.stream()
                        .map(Budget::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                        
                    if (budget.getAmount().compareTo(totalCategoryBudgets) < 0) {
                        throw new RuntimeException("Le budget global ne peut pas être inférieur à la somme des sous-budgets existants");
                    }
                }
            }
        }
        
        // Suite du code existant pour sauvegarde...
        Budget budgetToSave;
        if (existingBudget.isPresent()) {
            budgetToSave = existingBudget.get();
            budgetToSave.setAmount(budget.getAmount());
            budgetToSave.setAlertThresholdPercentage(budget.getAlertThresholdPercentage());
            budgetToSave.setUpdatedAt(Instant.now());
        } else {
            budgetToSave = budget;
            budgetToSave.setUser(user);
            budgetToSave.setCreatedAt(Instant.now());
            budgetToSave.setUpdatedAt(Instant.now());
        }
        
        return budgetRepository.save(budgetToSave);
    }
    
    public void deleteBudget(UserDetails userDetails, String budgetId) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new RuntimeException("Budget non trouvé"));
            
        if (!budget.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer ce budget");
        }
        
        budgetRepository.delete(budget);
    }
    
    // Vérification des budgets et alertes
    private void checkBudgetAlert(User user, ExpenseCategory category, LocalDate date, BigDecimal amount) {
        YearMonth yearMonth = YearMonth.from(date);
        
        // Vérifier le budget pour cette catégorie
        Optional<Budget> categoryBudget = budgetRepository.findByUserIdAndCategoryIdAndYearMonth(
            user.getId(), category.getId(), yearMonth
        );
        
        if (categoryBudget.isPresent()) {
            checkBudgetThreshold(user, categoryBudget.get(), category.getName());
        }
        
        // Vérifier le budget global
        Optional<Budget> globalBudget = budgetRepository.findByUserIdAndYearMonthAndCategoryIdIsNull(
            user.getId(), yearMonth
        );
        
        if (globalBudget.isPresent()) {
            checkBudgetThreshold(user, globalBudget.get(), "global");
        }
    }
    
    private void checkBudgetThreshold(User user, Budget budget, String budgetName) {
        YearMonth yearMonth = budget.getYearMonth();
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        BigDecimal totalExpenses;
        if (budget.getCategory() != null) {
            totalExpenses = expenseRepository.getTotalExpensesForUserAndCategoryInRange(
                user.getId(), budget.getCategory().getId(), startDate, endDate
            );
        } else {
            totalExpenses = expenseRepository.getTotalExpensesForUserInRange(
                user.getId(), startDate, endDate
            );
        }
        
        if (totalExpenses == null) {
            totalExpenses = BigDecimal.ZERO;
        }
        
        @SuppressWarnings("deprecation")
        int percentage = totalExpenses.multiply(new BigDecimal(100))
            .divide(budget.getAmount(), 0, BigDecimal.ROUND_HALF_UP)
            .intValue();
        
        // Vérifier si le seuil d'alerte est dépassé
        if (percentage >= budget.getAlertThresholdPercentage()) {
            try {
                String message;
                if (budget.getCategory() != null) {
                    message = "Alerte budget ! Vous avez dépensé " + percentage + "% de votre budget pour la catégorie '" + budgetName + "'.";
                } else {
                    message = "Alerte budget ! Vous avez dépensé " + percentage + "% de votre budget mensuel global.";
                }
                
                notificationService.createSystemNotification(
                    user,
                    "BUDGET_ALERT",
                    message,
                    "/finance/budget"
                );
            } catch (Exception e) {
                System.out.println("Erreur lors de l'envoi de l'alerte de budget: " + e.getMessage());
            }
        }
    }
    
    public Map<String, Object> getMonthlyFinancialSummary(UserDetails userDetails, YearMonth yearMonth) {
    User user = userRepository.findByEmail(userDetails.getUsername())
        .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    
    // Récupérer toutes les dépenses du mois
    List<Expense> expenses = expenseRepository.findByUserIdAndDateBetweenOrderByDateDesc(
        user.getId(), startDate, endDate
    );
    
    // Calculer le total des dépenses
    BigDecimal totalExpenses = expenses.stream()
        .map(Expense::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    // Récupérer tous les budgets
    List<Budget> budgets = budgetRepository.findByUserIdAndYearMonth(user.getId(), yearMonth);
    
    // Récupérer le budget global s'il existe
    Optional<Budget> globalBudget = budgets.stream()
        .filter(b -> b.getCategory() == null)
        .findFirst();
    
    // Récupérer les budgets par catégorie
    List<Budget> categoryBudgets = budgets.stream()
        .filter(b -> b.getCategory() != null)
        .collect(Collectors.toList());
    
    // Préparer le résumé
    Map<String, Object> summary = new HashMap<>();
    summary.put("month", yearMonth.toString());
    summary.put("totalExpenses", totalExpenses);
    
    if (globalBudget.isPresent()) {
        BigDecimal budgetAmount = globalBudget.get().getAmount();
        summary.put("budgetAmount", budgetAmount);
        summary.put("remaining", budgetAmount.subtract(totalExpenses));
        
        @SuppressWarnings("deprecation")
        int percentage = totalExpenses.multiply(new BigDecimal(100))
            .divide(budgetAmount, 0, BigDecimal.ROUND_HALF_UP)
            .intValue();
        summary.put("percentageUsed", percentage);
    }
    
    // Calculer les dépenses et budgets par catégorie
    Map<String, BigDecimal> expensesByCategory = new HashMap<>();
    Map<String, BigDecimal> budgetsByCategory = new HashMap<>();
    
    for (Expense expense : expenses) {
        String categoryName = expense.getCategory().getName();
        expensesByCategory.put(
            categoryName,
            expensesByCategory.getOrDefault(categoryName, BigDecimal.ZERO).add(expense.getAmount())
        );
    }
    
    for (Budget budget : categoryBudgets) {
        String categoryName = budget.getCategory().getName();
        budgetsByCategory.put(categoryName, budget.getAmount());
    }
    
    summary.put("byCategory", expensesByCategory);
    summary.put("budgetByCategory", budgetsByCategory);
    
    // Ajouter les dépenses récentes limitées à 5
    List<Expense> recentExpenses = expenses.stream()
        .sorted((e1, e2) -> e2.getDate().compareTo(e1.getDate()))
        .limit(5)
        .toList();
    summary.put("recentExpenses", recentExpenses);
    
    return summary;
}
    

    
}
