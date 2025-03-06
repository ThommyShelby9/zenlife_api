// FCMPushService.java
package com.api.expo.services;

import com.api.expo.models.PushSubscription;
import com.api.expo.models.User;
import com.api.expo.repository.PushSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FCMPushService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    @Value("${app.webpush.public.key}")
    private String publicKey;
    
    public FCMPushService(PushSubscriptionRepository pushSubscriptionRepository,
                          ObjectMapper objectMapper) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }
    
    @PostConstruct
    private void init() {
        // Initialisation si nécessaire
    }
    
    public String getPublicKey() {
        return publicKey;
    }
    
    public void saveSubscription(User user, Map<String, Object> subscriptionDto) {
        @SuppressWarnings("unchecked")
        Map<String, String> keys = (Map<String, String>) subscriptionDto.get("keys");
        
        PushSubscription subscription = new PushSubscription();
        subscription.setUser(user);
        subscription.setEndpoint((String) subscriptionDto.get("endpoint"));
        subscription.setP256dh(keys.get("p256dh"));
        subscription.setAuth(keys.get("auth"));
        
        pushSubscriptionRepository.save(subscription);
    }
    
    /**
     * Envoie une notification à un utilisateur en utilisant les API Web Push directement
     */
    public void sendNotification(User user, String title, String body, String icon, String tag, String url) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByUserId(user.getId());
        
        if (subscriptions.isEmpty()) {
            return; // Aucun abonnement pour cet utilisateur
        }
        
        try {
            for (PushSubscription subscription : subscriptions) {
                // Créer la charge utile (payload) de la notification
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("title", title);
                notificationData.put("body", body);
                notificationData.put("icon", icon != null ? icon : "/img/logo.png");
                notificationData.put("badge", "/img/logo.png");
                notificationData.put("tag", tag);
                notificationData.put("data", Map.of("url", url != null ? url : "/"));
                
                // Si l'endpoint est pour FCM, on utilise le format FCM
                if (subscription.getEndpoint().contains("fcm.googleapis.com")) {
                    sendFCMNotification(subscription, notificationData);
                } else {
                    // Sinon on utilise l'API Web Push standard via HTTP
                    sendWebPushNotification(subscription, notificationData);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de la notification push: " + e.getMessage());
        }
    }
    
    /**
     * Envoie via FCM
     */
    private void sendFCMNotification(PushSubscription subscription, Map<String, Object> notificationData) {
        try {
            // Extraire l'ID de l'endpoint FCM
            String endpoint = subscription.getEndpoint();
            String fcmToken;
            
            // Correction de l'extraction du token
            if (endpoint.contains("fcm.googleapis.com/fcm/send/")) {
                fcmToken = endpoint.substring(endpoint.lastIndexOf("/") + 1);
            } else {
                throw new IllegalArgumentException("Endpoint invalide pour FCM");
            }
    
            InputStream serviceAccount = getClass().getResourceAsStream("/zenlife-b7b30-firebase-adminsdk-fbsvc-87b92dbb99.json");
            if (serviceAccount == null) {
                throw new IllegalStateException("Fichier de configuration Firebase Admin introuvable");
            }
            
            GoogleCredentials googleCredentials = GoogleCredentials.fromStream(serviceAccount)
                    .createScoped(Arrays.asList("https://www.googleapis.com/auth/firebase.messaging"));
            googleCredentials.refreshIfExpired();
            String token = googleCredentials.getAccessToken().getTokenValue();
            
            // Préparer les en-têtes
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);
            
            // Extraire les valeurs nécessaires du notificationData
            String title = (String) notificationData.getOrDefault("title", "ZenLife");
            String body = (String) notificationData.getOrDefault("body", "");
            
            // Structure correcte pour FCM
            Map<String, Object> fcmRequest = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            Map<String, Object> notification = new HashMap<>();
            Map<String, Object> android = new HashMap<>();
            Map<String, Object> apns = new HashMap<>();
            Map<String, Object> webpush = new HashMap<>();
            
            // Configuration de la notification (uniquement les champs autorisés par FCM)
            notification.put("title", title);
            notification.put("body", body);
            
            // Données supplémentaires doivent être placées dans le champ "data"
            Map<String, String> data = new HashMap<>();
            if (notificationData.containsKey("url")) {
                data.put("url", (String) notificationData.get("url"));
            }
            if (notificationData.containsKey("tag")) {
                data.put("tag", (String) notificationData.get("tag"));
            }
            
            // Configuration spécifique à Android
            Map<String, Object> androidNotification = new HashMap<>();
            if (notificationData.containsKey("icon")) {
                androidNotification.put("icon", notificationData.get("icon"));
            }
            androidNotification.put("click_action", "FLUTTER_NOTIFICATION_CLICK");
            android.put("notification", androidNotification);
            
            // Configuration spécifique à Web
            Map<String, Object> webpushNotification = new HashMap<>();
            if (notificationData.containsKey("icon")) {
                webpushNotification.put("icon", notificationData.get("icon"));
            }
            if (notificationData.containsKey("badge")) {
                webpushNotification.put("badge", notificationData.get("badge"));
            }
            if (notificationData.containsKey("tag")) {
                webpushNotification.put("tag", notificationData.get("tag"));
            }
            webpush.put("notification", webpushNotification);
            
            // Configuration APNS (iOS)
            Map<String, Object> apnsPayload = new HashMap<>();
            Map<String, Object> aps = new HashMap<>();
            aps.put("content-available", 1);
            aps.put("mutable-content", 1);
            apnsPayload.put("aps", aps);
            apns.put("payload", apnsPayload);
            apns.put("headers", Map.of("apns-priority", "10"));
            
            // Assemblage du message complet
            message.put("token", fcmToken);
            message.put("notification", notification);
            message.put("data", data);
            message.put("android", android);
            message.put("webpush", webpush);
            message.put("apns", apns);
            
            fcmRequest.put("message", message);
            
            // Envoyer la requête
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(fcmRequest, headers);
            restTemplate.postForEntity("https://fcm.googleapis.com/v1/projects/zenlife-b7b30/messages:send", request, String.class);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi FCM: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * Envoi via l'API Web Push standard
     */
    private void sendWebPushNotification(PushSubscription subscription, Map<String, Object> notificationData) {
        try {
            // Préparer les en-têtes pour l'API Web Push
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "vapid t=" + publicKey + ", k=" + subscription.getP256dh());
            headers.set("Crypto-Key", "p256ecdsa=" + subscription.getP256dh());
            
            // Convertir les données en JSON
            String payload = objectMapper.writeValueAsString(notificationData);
            
            // Créer la requête
            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            
            // Envoyer la requête
            restTemplate.postForEntity(subscription.getEndpoint(), request, String.class);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi Web Push: " + e.getMessage());
        }
    }
}