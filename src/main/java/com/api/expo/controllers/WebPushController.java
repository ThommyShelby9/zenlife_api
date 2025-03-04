// WebPushController.java
package com.api.expo.controllers;

import com.api.expo.models.User;
import com.api.expo.repository.UserRepository;
import com.api.expo.services.FCMPushService; // Service de remplacement
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*", maxAge = 3600)
public class WebPushController {

    private final FCMPushService pushService;
    private final UserRepository userRepository;

    public WebPushController(FCMPushService pushService, UserRepository userRepository) {
        this.pushService = pushService;
        this.userRepository = userRepository;
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
}