package com.api.expo.seeders;

import com.api.expo.models.ExpenseCategory;
import com.api.expo.models.User;
import com.api.expo.repository.ExpenseCategoryRepository;
import com.api.expo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ExpenseCategorySeeder {

    @Bean
    @Transactional
    public CommandLineRunner seedExpenseCategories(
            ExpenseCategoryRepository expenseCategoryRepository, 
            UserRepository userRepository) {
        return args -> {
            // Vérifier si des catégories existent déjà
            if (expenseCategoryRepository.count() > 0) {
                return;
            }


            // Liste des catégories de dépenses prédéfinies
            List<ExpenseCategory> defaultCategories = Arrays.asList(
                // Catégories de base
                createCategory( "Alimentation", "🍽️", "#FF6B6B"),
                createCategory( "Transport", "🚗", "#4ECDC4"),
                createCategory( "Logement", "🏠", "#45B7D1"),
                createCategory( "Loisirs", "🎉", "#FDCB6E"),
                createCategory( "Courses", "🛒", "#6C5CE7"),
                createCategory( "Santé", "🏥", "#A8E6CF"),
                createCategory( "Éducation", "📚", "#FF8ED4"),
                createCategory( "Vêtements", "👗", "#6A5ACD"),
                createCategory( "Abonnements", "💳", "#FAD390"),
                createCategory( "Divers", "💡", "#D1D8E0")
            );

            // Sauvegarder toutes les catégories
            expenseCategoryRepository.saveAll(defaultCategories);
        };
    }

    /**
     * Méthode utilitaire pour créer une catégorie de dépense
     * 
     * @param name Nom de la catégorie
     * @param icon Icône représentant la catégorie
     * @param color Couleur associée à la catégorie
     * @return ExpenseCategory nouvellement créée
     */
    private ExpenseCategory createCategory(String name, String icon, String color) {
        ExpenseCategory category = new ExpenseCategory();
        category.setName(name);
        category.setIcon(icon);
        category.setColor(color);
        return category;
    }
}