package com.api.expo.services;

import com.api.expo.models.PositiveThought;
import com.api.expo.models.User;
import com.api.expo.models.UserPositiveThoughtSetting;
import com.api.expo.repository.PositiveThoughtRepository;
import com.api.expo.repository.UserPositiveThoughtSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PositiveThoughtNotificationService {
    
    private final PositiveThoughtRepository positiveThoughtRepository;
    private final UserPositiveThoughtSettingRepository userPositiveThoughtSettingRepository;
    private final NotificationService notificationService;
    
    /**
     * Envoyer des notifications de pensées positives toutes les minutes
     * Le système vérifie chaque minute quels utilisateurs doivent recevoir une notification
     */
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void sendScheduledPositiveThoughts() {
        // Récupérer tous les paramètres utilisateurs avec notifications activées
        List<UserPositiveThoughtSetting> allSettings = userPositiveThoughtSettingRepository.findByEnabledTrue();
        
        for (UserPositiveThoughtSetting settings : allSettings) {
            try {
                // Vérifier si c'est le moment d'envoyer une notification
                if (shouldSendNotification(settings)) {
                    sendPositiveThoughtNotification(settings);
                    
                    // Mettre à jour la date du dernier envoi
                    settings.setLastNotificationSentAt(Instant.now());
                    userPositiveThoughtSettingRepository.save(settings);
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de l'envoi d'une pensée positive à l'utilisateur " + 
                    settings.getUser().getId() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Détermine si une notification doit être envoyée en fonction des paramètres utilisateur
     */
    private boolean shouldSendNotification(UserPositiveThoughtSetting settings) {
        // Si les notifications ne sont pas activées, ne pas envoyer
        if (!settings.getEnabled()) {
            return false;
        }
        
        // Si c'est le premier envoi (lastNotificationSentAt est null)
        if (settings.getLastNotificationSentAt() == null) {
            return true;
        }
        
        // Calculer l'intervalle en minutes en fonction de la fréquence
        int frequencyPerDay = settings.getFrequencyPerDay();
        int intervalInMinutes;
        
        if (frequencyPerDay <= 0) {
            // Par défaut, une fois par jour
            intervalInMinutes = 24 * 60;
        } else {
            // Convertir la fréquence par jour en intervalle en minutes
            intervalInMinutes = (24 * 60) / frequencyPerDay;
        }
        
        // Vérifier si l'intervalle requis s'est écoulé depuis le dernier envoi
        Instant lastSent = settings.getLastNotificationSentAt();
        Instant now = Instant.now();
        long minutesElapsed = Duration.between(lastSent, now).toMinutes();
        
        return minutesElapsed >= intervalInMinutes;
    }
    
    /**
     * Envoyer une notification de pensée positive
     */
    public void sendPositiveThoughtNotification(UserPositiveThoughtSetting settings) {
        User user = settings.getUser();
        
        // Sélectionner une pensée en fonction des préférences de catégorie
        PositiveThought thought = selectPositiveThought(settings.getPreferredCategories());
        
        if (thought != null) {
            // Créer les données de la notification
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("type", "POSITIVE_THOUGHT");
            notificationData.put("content", thought.getContent());
            notificationData.put("author", thought.getAuthor());
            notificationData.put("category", thought.getCategory());
            notificationData.put("thought", thought);
            notificationData.put("link", "/positive-thoughts");
            notificationData.put("displayOnLockScreen", settings.getDisplayOnLockScreen());
            
            // Envoyer la notification via WebSocket pour les utilisateurs connectés
            notificationService.createSystemNotification(
                user,
                "POSITIVE_THOUGHT",
                thought.getContent(),
                "/positive-thoughts",
                notificationData
            );
        }
    }
    
    /**
     * Sélectionner une pensée positive en fonction des préférences de catégorie
     */
    private PositiveThought selectPositiveThought(String preferredCategories) {
        if ("all".equals(preferredCategories)) {
            // Sélectionner une pensée aléatoire
            return positiveThoughtRepository.findRandom();
        } else {
            // Sélectionner une catégorie aléatoire parmi les préférées
            String[] categories = preferredCategories.split(",");
            if (categories.length > 0) {
                String randomCategory = categories[new Random().nextInt(categories.length)];
                PositiveThought thought = positiveThoughtRepository.findRandomByCategory(randomCategory);
                
                // Si aucune pensée n'est trouvée dans cette catégorie, prendre une pensée aléatoire
                if (thought == null) {
                    return positiveThoughtRepository.findRandom();
                }
                
                return thought;
            } else {
                // Si pas de catégories préférées, sélectionner une pensée aléatoire
                return positiveThoughtRepository.findRandom();
            }
        }
    }
}