// WebPushController.java
package com.api.expo.controllers;

import com.api.expo.models.PushSubscription;
import com.api.expo.models.User;
import com.api.expo.repository.PushSubscriptionRepository;
import com.api.expo.repository.UserRepository;
import com.api.expo.services.FCMPushService; // Service de remplacement

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*", maxAge = 3600)
public class WebPushController {

    private final FCMPushService pushService;
    private final UserRepository userRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;

    public WebPushController(FCMPushService pushService, UserRepository userRepository, PushSubscriptionRepository pushSubscriptionRepository) {
            this.pushService = pushService;
            this.userRepository = userRepository;
            this.pushSubscriptionRepository = pushSubscriptionRepository;
    }

    @GetMapping("/public-key")
    public ResponseEntity<?> getPublicKey() {
        Map<String, String> response = new HashMap<>();
        response.put("publicKey", pushService.getPublicKey());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(
            @RequestBody Map<String, Object> subscription,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            // Récupérer l'utilisateur
            User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            // Enregistrer l'abonnement
            pushService.saveSubscription(user, subscription);
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // Implémenter la logique de désabonnement
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    @PostMapping("/test")
    public ResponseEntity<?> testNotification(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Récupérer l'utilisateur
            User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            // Envoyer une notification de test
            pushService.sendNotification(
                user,
                "Test de notification",
                "Ceci est une notification de test",
                null,
                "test-notification",
                "/dashboard"
            );
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/test-detailed")
public ResponseEntity<?> testDetailedNotification(@AuthenticationPrincipal UserDetails userDetails) {
    try {
        // Récupérer l'utilisateur
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        System.out.println("Test de notification pour l'utilisateur: " + user.getUsername());
        
        // Récupérer les abonnements
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByUserId(user.getId());
        System.out.println("Nombre d'abonnements trouvés: " + subscriptions.size());
        
        if (subscriptions.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Aucun abonnement trouvé pour cet utilisateur"
            ));
        }
        
        // Envoyer une notification de test avec plus de détails
        for (PushSubscription subscription : subscriptions) {
            System.out.println("Envoi à l'endpoint: " + subscription.getEndpoint());
            
            pushService.sendNotification(
                user,
                "Test de notification détaillé",
                "Ceci est une notification de test avec logs détaillés",
                null,
                "test-notification-detailed",
                "/dashboard"
            );
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Notification de test envoyée avec logs détaillés"
        ));
    } catch (Exception e) {
        System.err.println("Erreur détaillée lors du test de notification: " + e.getMessage());
        e.printStackTrace();
        
        return ResponseEntity.internalServerError().body(Map.of(
            "success", false,
            "error", e.getMessage(),
            "stackTrace", e.getStackTrace()
        ));
    }
}
}