package com.api.expo.controllers;

import com.api.expo.models.User;
import com.api.expo.models.UserPositiveThoughtSetting;
import com.api.expo.repository.UserRepository;
import com.api.expo.services.PositiveThoughtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class PositiveThoughtNotificationController {
    
    private final PositiveThoughtService positiveThoughtService;
    private final UserRepository userRepository;
    
    /**
     * Endpoint pour s'abonner aux notifications de pensées positives
     */
    @PostMapping("/subscribe/positive-thoughts")
    public ResponseEntity<?> subscribeToPositiveThoughts(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Récupérer l'utilisateur
            User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            // Extraire les paramètres de notification du payload
            boolean notificationEnabled = payload.containsKey("notificationEnabled") ? 
                    (boolean) payload.get("notificationEnabled") : true;
            
            String frequency = payload.containsKey("frequency") ? 
                    (String) payload.get("frequency") : "daily";
            
            Integer customInterval = payload.containsKey("customInterval") ? 
                    ((Number) payload.get("customInterval")).intValue() : 60;
            
            boolean displayOnLockScreen = payload.containsKey("displayOnLockScreen") ? 
                    (boolean) payload.get("displayOnLockScreen") : false;
            
            // Traitement des catégories préférées
            String preferredCategories = "all";
            if (payload.containsKey("preferredCategories")) {
                if (payload.get("preferredCategories") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> categoriesList = (List<String>) payload.get("preferredCategories");
                    if (!categoriesList.isEmpty()) {
                        preferredCategories = String.join(",", categoriesList);
                    }
                } else if (payload.get("preferredCategories") instanceof String) {
                    preferredCategories = (String) payload.get("preferredCategories");
                }
            }
            
            // Calculer la fréquence par jour
            int frequencyPerDay = calculateFrequencyPerDay(frequency, customInterval);
            
            // Créer ou mettre à jour les paramètres utilisateur
            UserPositiveThoughtSetting settings = new UserPositiveThoughtSetting();
            settings.setUser(user);
            settings.setEnabled(true);
            settings.setFrequencyPerDay(frequencyPerDay);
            settings.setPreferredCategories(preferredCategories);
            settings.setNotificationEnabled(notificationEnabled);
            settings.setDisplayOnLockScreen(displayOnLockScreen);
            settings.setCreatedAt(Instant.now());
            settings.setUpdatedAt(Instant.now());
            
            // Sauvegarder les paramètres
            UserPositiveThoughtSetting updatedSettings = positiveThoughtService.updateUserSettings(userDetails, settings);
            
            // Construire la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Abonnement aux notifications de pensées positives effectué avec succès");
            response.put("settings", updatedSettings);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de l'abonnement aux notifications");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Utilitaire pour calculer la fréquence quotidienne des notifications
     */
    private int calculateFrequencyPerDay(String frequency, int customInterval) {
        switch (frequency) {
            case "hourly":
                return 24; // Une fois par heure
            case "daily":
                return 1; // Une fois par jour
            case "custom":
                if (customInterval <= 0) {
                    return 1; // Valeur par défaut
                }
                // Convertir les minutes en fréquence par jour
                return Math.max(1, Math.min(144, (24 * 60) / customInterval));
            default:
                return 1;
        }
    }
}