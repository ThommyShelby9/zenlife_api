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

            // Récupérer un utilisateur par défaut (à adapter selon votre logique d'authentification)
            User defaultUser = userRepository.findById("f984462e-c988-4718-977f-98d4f88acc43")
                    .orElseThrow(() -> new RuntimeException("Utilisateur par défaut non trouvé"));

            // Liste des catégories de dépenses prédéfinies
            List<ExpenseCategory> defaultCategories = Arrays.asList(
                // Catégories de base
                createCategory(defaultUser, "Alimentation", "🍽️", "#FF6B6B"),
                createCategory(defaultUser, "Transport", "🚗", "#4ECDC4"),
                createCategory(defaultUser, "Logement", "🏠", "#45B7D1"),
                createCategory(defaultUser, "Loisirs", "🎉", "#FDCB6E"),
                createCategory(defaultUser, "Courses", "🛒", "#6C5CE7"),
                createCategory(defaultUser, "Santé", "🏥", "#A8E6CF"),
                createCategory(defaultUser, "Éducation", "📚", "#FF8ED4"),
                createCategory(defaultUser, "Vêtements", "👗", "#6A5ACD"),
                createCategory(defaultUser, "Abonnements", "💳", "#FAD390"),
                createCategory(defaultUser, "Divers", "💡", "#D1D8E0")
            );

            // Sauvegarder toutes les catégories
            expenseCategoryRepository.saveAll(defaultCategories);
        };
    }

    /**
     * Méthode utilitaire pour créer une catégorie de dépense
     * 
     * @param user Utilisateur propriétaire de la catégorie
     * @param name Nom de la catégorie
     * @param icon Icône représentant la catégorie
     * @param color Couleur associée à la catégorie
     * @return ExpenseCategory nouvellement créée
     */
    private ExpenseCategory createCategory(User user, String name, String icon, String color) {
        ExpenseCategory category = new ExpenseCategory();
        category.setUser(user);
        category.setName(name);
        category.setIcon(icon);
        category.setColor(color);
        return category;
    }
}