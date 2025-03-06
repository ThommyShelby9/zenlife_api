// FCMPushService.java
package com.api.expo.services;

import com.api.expo.models.Notification;
import com.api.expo.models.PushSubscription;
import com.api.expo.models.User;
import com.api.expo.repository.NotificationRepository;
import com.api.expo.repository.PushSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FCMPushService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    @Value("${app.webpush.public.key}")
    private String publicKey;
    
    public FCMPushService(PushSubscriptionRepository pushSubscriptionRepository,
                          NotificationRepository notificationRepository,
                          ObjectMapper objectMapper) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.notificationRepository = notificationRepository;
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
            System.out.println("Aucun abonnement trouvé pour l'utilisateur " + user.getUsername());
            return;
        }
        
        System.out.println("Envoi de notification à " + user.getUsername() + " - " + subscriptions.size() + " abonnement(s) trouvé(s)");
        
        try {
            boolean notificationSent = false;
            
            // Essayer chaque abonnement en séquence
            for (PushSubscription subscription : subscriptions) {
                if (notificationSent) break; // Sortir si une notification a déjà été envoyée avec succès
                
                try {
                    // Créer la charge utile (payload) de la notification
                    Map<String, Object> notificationData = new HashMap<>();
                    notificationData.put("title", title);
                    notificationData.put("body", body);
                    notificationData.put("icon", icon != null ? icon : "/img/logo.png");
                    notificationData.put("badge", "/img/logo.png");
                    notificationData.put("tag", tag);
                    notificationData.put("data", Map.of("url", url != null ? url : "/"));
                    
                    // Méthode 1: Essayer d'abord Web Push standard
                    try {
                        boolean webPushSuccess = sendWebPushNotification(subscription, notificationData);
                        if (webPushSuccess) {
                            System.out.println("Notification envoyée avec succès via Web Push pour " + user.getUsername());
                            notificationSent = true;
                            continue; // Passer à l'abonnement suivant si celui-ci a échoué
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur lors de l'envoi via Web Push: " + e.getMessage());
                    }
                    
                    // Méthode 2: Si Web Push a échoué, essayer FCM
                    try {
                        if (subscription.getEndpoint().contains("fcm.googleapis.com")) {
                            System.out.println("Tentative d'envoi via FCM pour " + user.getUsername());
                            sendFCMNotification(subscription, notificationData);
                            // Supposons que FCM a réussi si aucune exception n'est levée
                            System.out.println("Notification envoyée avec succès via FCM pour " + user.getUsername());
                            notificationSent = true;
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur lors de l'envoi via FCM: " + e.getMessage());
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de la préparation de la notification: " + e.getMessage());
                }
            }
            
            // Si aucune notification n'a réussi, sauvegarder en base de données
            if (!notificationSent && notificationRepository != null) {
                System.out.println("Aucune notification push n'a pu être envoyée. Stockage en base de données.");
                // Créer une notification dans la base de données qui sera affichée quand l'utilisateur
                // se connectera la prochaine fois
                Notification notification = new Notification();
                notification.setUser(user);
                notification.setType("POSITIVE_THOUGHT");
                notification.setContent(body);
                notification.setLink(url);
                notification.setCreatedAt(Instant.now());
                notification.setRead(false);
                notificationRepository.save(notification);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de la notification push: " + e.getMessage());
            e.printStackTrace();
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
            if (notificationData.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, String> originalData = (Map<String, String>) notificationData.get("data");
                data.putAll(originalData);
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
            throw new RuntimeException("Échec de l'envoi FCM", e);
        }
    }
    
    /**
     * Envoi via l'API Web Push standard
     */
    private boolean sendWebPushNotification(PushSubscription subscription, Map<String, Object> notificationData) {
        try {
            // Vérifier si l'endpoint est valide
            String endpoint = subscription.getEndpoint();
            if (endpoint == null || endpoint.isEmpty()) {
                System.err.println("Endpoint invalide pour la notification Web Push");
                return false;
            }
            
            System.out.println("Envoi de notification Web Push à: " + endpoint);

            // Préparer le payload selon la spécification Web Push
            Map<String, Object> payload = new HashMap<>();
            payload.put("notification", notificationData);
            
            // Convertir en JSON
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            // Préparer les en-têtes HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Ajouter les en-têtes d'authentification Web Push
            String vapidAuthHeader = "vapid t=" + publicKey + ", k=" + subscription.getP256dh();
            headers.set("Authorization", vapidAuthHeader);
            
            // Option: Ajouter une durée de vie à la notification
            headers.set("TTL", "86400");  // 24 heures en secondes
            
            // Créer la requête
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
            
            // Utiliser un client HTTP avec un timeout plus long
            RestTemplate customTemplate = new RestTemplate();
            customTemplate.setRequestFactory(new SimpleClientHttpRequestFactory());
            ((SimpleClientHttpRequestFactory) customTemplate.getRequestFactory()).setConnectTimeout(10000);
            ((SimpleClientHttpRequestFactory) customTemplate.getRequestFactory()).setReadTimeout(10000);
            
            // Envoyer la requête et afficher le résultat
            ResponseEntity<String> response = customTemplate.postForEntity(endpoint, request, String.class);
            
            System.out.println("Réponse du serveur Web Push: " + response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("Erreur détaillée lors de l'envoi Web Push: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}