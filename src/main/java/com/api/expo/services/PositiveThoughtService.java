// PositiveThoughtService.java
package com.api.expo.services;

import com.api.expo.models.PositiveThought;
import com.api.expo.models.User;
import com.api.expo.models.UserPositiveThoughtSetting;
import com.api.expo.repository.PositiveThoughtRepository;
import com.api.expo.repository.UserPositiveThoughtSettingRepository;
import com.api.expo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PositiveThoughtService {
    
    private final PositiveThoughtRepository positiveThoughtRepository;
    private final UserPositiveThoughtSettingRepository userPositiveThoughtSettingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    public PositiveThought getRandomPositiveThought(String category) {
        if (category != null && !category.isEmpty() && !category.equals("all")) {
            return positiveThoughtRepository.findRandomByCategory(category);
        } else {
            return positiveThoughtRepository.findRandom();
        }
    }
    
    public List<PositiveThought> getAllPositiveThoughts() {
        return positiveThoughtRepository.findAll();
    }
    
    public List<PositiveThought> getPositiveThoughtsByCategory(String category) {
        return positiveThoughtRepository.findByCategory(category);
    }
    
    public PositiveThought createPositiveThought(PositiveThought thought) {
        thought.setCreatedAt(Instant.now());
        return positiveThoughtRepository.save(thought);
    }
    
    public UserPositiveThoughtSetting getUserSettings(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        Optional<UserPositiveThoughtSetting> settings = userPositiveThoughtSettingRepository.findByUserId(user.getId());
        if (settings.isPresent()) {
            return settings.get();
        } else {
            // Créer des paramètres par défaut
            UserPositiveThoughtSetting defaultSettings = new UserPositiveThoughtSetting();
            defaultSettings.setUser(user);
            defaultSettings.setEnabled(true);
            defaultSettings.setFrequencyPerDay(3);
            defaultSettings.setPreferredCategories("all");
            
            return userPositiveThoughtSettingRepository.save(defaultSettings);
        }
    }
    
    public UserPositiveThoughtSetting updateUserSettings(UserDetails userDetails, UserPositiveThoughtSetting updatedSettings) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        Optional<UserPositiveThoughtSetting> existingSettings = userPositiveThoughtSettingRepository.findByUserId(user.getId());
        
        UserPositiveThoughtSetting settingsToSave;
        if (existingSettings.isPresent()) {
            settingsToSave = existingSettings.get();
            settingsToSave.setEnabled(updatedSettings.getEnabled());
            settingsToSave.setFrequencyPerDay(updatedSettings.getFrequencyPerDay());
            settingsToSave.setPreferredCategories(updatedSettings.getPreferredCategories());
            settingsToSave.setUpdatedAt(Instant.now());
        } else {
            settingsToSave = updatedSettings;
            settingsToSave.setUser(user);
            settingsToSave.setCreatedAt(Instant.now());
            settingsToSave.setUpdatedAt(Instant.now());
        }
        
        return userPositiveThoughtSettingRepository.save(settingsToSave);
    }
    
    public void sendPositiveThought(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        Optional<UserPositiveThoughtSetting> settingsOpt = userPositiveThoughtSettingRepository.findByUserId(userId);
        if (settingsOpt.isPresent() && settingsOpt.get().getEnabled()) {
            String preferredCategories = settingsOpt.get().getPreferredCategories();
            
            PositiveThought thought;
            if (preferredCategories.equals("all")) {
                thought = positiveThoughtRepository.findRandom();
            } else {
                // Sélectionner une catégorie aléatoire parmi les préférées
                String[] categories = preferredCategories.split(",");
                String randomCategory = categories[new Random().nextInt(categories.length)];
                thought = positiveThoughtRepository.findRandomByCategory(randomCategory);
                
                // Si aucune pensée n'est trouvée dans cette catégorie, prendre une pensée aléatoire
                if (thought == null) {
                    thought = positiveThoughtRepository.findRandom();
                }
            }
            
            if (thought != null) {
                try {
                    notificationService.createSystemNotification(
                        user,
                        "POSITIVE_THOUGHT",
                        "✨ " + thought.getContent(),
                        "/positive-thoughts"
                    );
                } catch (Exception e) {
                    System.out.println("Erreur lors de l'envoi de la pensée positive: " + e.getMessage());
                }
            }
        }
    }
}