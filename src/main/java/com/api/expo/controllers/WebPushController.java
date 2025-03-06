package com.api.expo.controllers;

import com.api.expo.models.PushSubscription;
import com.api.expo.models.User;
import com.api.expo.repository.PushSubscriptionRepository;
import com.api.expo.repository.UserRepository;
import com.api.expo.services.FCMPushService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            
            // Extraire l'endpoint
            String endpoint = (String) subscription.get("endpoint");
            if (endpoint == null || endpoint.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Endpoint manquant dans la requête"
                ));
            }

            // Vérifier si cet endpoint existe déjà pour cet utilisateur
            Optional<PushSubscription> existingSubscription = pushSubscriptionRepository.findByEndpoint(endpoint);
            if (existingSubscription.isPresent()) {
                PushSubscription sub = existingSubscription.get();
                
                // Si l'abonnement appartient déjà à cet utilisateur, le mettre à jour
                if (sub.getUser().getId().equals(user.getId())) {
                    // Mise à jour de la date et des clés si nécessaire
                    updateSubscriptionKeys(sub, subscription);
                    sub.setUpdatedAt(Instant.now());
                    pushSubscriptionRepository.save(sub);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Abonnement mis à jour"));
                } else {
                    // Si l'endpoint est déjà utilisé par un autre utilisateur, le remplacer
                    sub.setUser(user);
                    updateSubscriptionKeys(sub, subscription);
                    sub.setUpdatedAt(Instant.now());
                    pushSubscriptionRepository.save(sub);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Abonnement transféré"));
                }
            }
            
            // Créer un nouvel abonnement
            PushSubscription newSubscription = new PushSubscription();
            newSubscription.setUser(user);
            newSubscription.setEndpoint(endpoint);
            
            // Extraire les clés si présentes
            updateSubscriptionKeys(newSubscription, subscription);
            
            // Définir les dates
            newSubscription.setCreatedAt(Instant.now());
            newSubscription.setUpdatedAt(Instant.now());
            
            // Sauvegarder l'abonnement
            pushSubscriptionRepository.save(newSubscription);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Abonnement créé avec succès"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    private void updateSubscriptionKeys(PushSubscription subscription, Map<String, Object> data) {
        // Gérer le cas où les clés sont fournies directement ou sous l'objet 'keys'
        @SuppressWarnings("unchecked")
        Map<String, String> keys = data.containsKey("keys") ? 
            (Map<String, String>) data.get("keys") : new HashMap<>();
        
        // Extraire les clés p256dh et auth
        String p256dh = keys.getOrDefault("p256dh", 
            data.containsKey("p256dh") ? (String) data.get("p256dh") : "FCM-key");
        
        String auth = keys.getOrDefault("auth", 
            data.containsKey("auth") ? (String) data.get("auth") : "FCM-auth");
        
        subscription.setP256dh(p256dh);
        subscription.setAuth(auth);
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            // Récupérer l'utilisateur
            User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            // Extraire l'endpoint
            String endpoint = payload.get("endpoint");
            if (endpoint == null || endpoint.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Endpoint manquant dans la requête"
                ));
            }
            
            // Rechercher l'abonnement
            Optional<PushSubscription> subscription = pushSubscriptionRepository.findByEndpoint(endpoint);
            if (subscription.isPresent()) {
                // Vérifier que l'abonnement appartient à l'utilisateur actuel
                PushSubscription sub = subscription.get();
                if (sub.getUser().getId().equals(user.getId())) {
                    // Supprimer l'abonnement
                    pushSubscriptionRepository.delete(sub);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Abonnement supprimé avec succès"));
                } else {
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Cet abonnement appartient à un autre utilisateur"
                    ));
                }
            } else {
                // L'abonnement n'existe pas, considérer comme un succès
                return ResponseEntity.ok(Map.of("success", true, "message", "Abonnement déjà supprimé"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/test")
    public ResponseEntity<?> testNotification(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Récupérer l'utilisateur
            User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            // Récupérer les abonnements
            List<PushSubscription> subscriptions = pushSubscriptionRepository.findByUserId(user.getId());
            
            if (subscriptions.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Aucun abonnement trouvé. Veuillez d'abord activer les notifications push."
                ));
            }
            
            // Envoyer une notification de test
            pushService.sendNotification(
                user,
                "Notification de test",
                "Félicitations ! Vous recevrez maintenant des pensées positives même lorsque l'application est fermée.",
                "/img/logo.png",
                "test-notification",
                "/positive-thoughts"
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Notification de test envoyée avec succès",
                "subscriptionCount", subscriptions.size()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}