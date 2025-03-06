package com.api.expo.services;

import com.api.expo.models.Notification;
import com.api.expo.models.PushSubscription;
import com.api.expo.models.User;
import com.api.expo.repository.NotificationRepository;
import com.api.expo.repository.PushSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;

import io.jsonwebtoken.Jwts;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.jose4j.lang.JoseException;

import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Date;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.net.URL;
import java.net.URI;

@Service
public class FCMPushService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    @Value("${app.webpush.public.key}")
    private String publicKey;
    
    @Value("${app.webpush.private.key}")
    private String privateKey;
    
    @Value("${app.webpush.subject}")
    private String subject;
    
    public FCMPushService(PushSubscriptionRepository pushSubscriptionRepository,
                          NotificationRepository notificationRepository,
                          ObjectMapper objectMapper) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
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
            // Extraire l'ID de l'endpoint FCM avec une expression régulière plus robuste
            String endpoint = subscription.getEndpoint();
            String fcmToken = extractFcmToken(endpoint);
            
            if (fcmToken == null || fcmToken.isEmpty()) {
                throw new IllegalArgumentException("Impossible d'extraire un token FCM valide de l'endpoint: " + endpoint);
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
            String fcmUrl = "https://fcm.googleapis.com/v1/projects/zenlife-b7b30/messages:send";
            System.out.println("Envoi FCM à " + fcmUrl + " avec token: " + fcmToken.substring(0, Math.min(fcmToken.length(), 10)) + "...");
            
            ResponseEntity<String> response = restTemplate.postForEntity(fcmUrl, request, String.class);
            System.out.println("Réponse FCM: " + response.getStatusCode());
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi FCM: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Échec de l'envoi FCM", e);
        }
    }
    
    /**
     * Extraire le token FCM avec une expression régulière plus robuste
     */
    private String extractFcmToken(String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) {
            return null;
        }
        
        // Utiliser une expression régulière pour extraire le token
        Pattern pattern = Pattern.compile("fcm\\.googleapis\\.com/fcm/send/([^/]+)$");
        Matcher matcher = pattern.matcher(endpoint);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Support pour d'autres formats possibles
        String[] parts = endpoint.split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        
        return null;
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
    
            // Préparer le payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("notification", notificationData);
            
            // Convertir en JSON
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            // Préparer les en-têtes HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Générer un JWT VAPID conforme
            String vapidJwt = generateVAPIDJWT(endpoint);
            headers.set("Authorization", "vapid t=" + vapidJwt + ", k=" + publicKey);
            
            // Ajouter la durée de vie
            headers.set("TTL", "86400");
            
            // Crypter le payload si nécessaire (pour les endpoints qui le requièrent)
            // Note: Cette partie est complexe et nécessiterait l'implémentation du cryptage ECDH
            // Pour simplifier, nous envoyons le payload sans cryptage pour les endpoints qui l'acceptent
            
            // Créer la requête
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
            
            // Configurer un client avec timeout plus long
            RestTemplate customTemplate = new RestTemplate();
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10000);
            factory.setReadTimeout(10000);
            customTemplate.setRequestFactory(factory);
            
            // Envoyer la requête
            ResponseEntity<String> response = customTemplate.postForEntity(endpoint, request, String.class);
            
            System.out.println("Réponse Web Push: " + response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("Erreur détaillée lors de l'envoi Web Push: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

   private String generateVAPIDJWT(String endpoint) {
    try {
        // Utiliser JJWT directement sans essayer de convertir la clé privée en PKCS#8
        long expirationTimeMillis = System.currentTimeMillis() + 86400 * 1000; // 24 heures
        Date expirationDate = new Date(expirationTimeMillis);
        
        // Extraire l'origine de l'endpoint pour le champ 'aud'
        URL endpointUrl = new URI(endpoint).toURL();
        String audience = endpointUrl.getProtocol() + "://" + endpointUrl.getHost();
        
        // Créer une clé secrète à partir de la clé privée VAPID directement
        SecretKey secretKey = new SecretKeySpec(
            Base64.getDecoder().decode(privateKey.replace('-', '+').replace('_', '/')), 
            "HmacSHA256"
        );
        
        // Créer le JWT avec HMAC-SHA256 au lieu d'ECDSA
        return Jwts.builder()
                .setSubject(subject)
                .claim("aud", audience)
                .setExpiration(expirationDate)
                .signWith(secretKey)
                .compact();
    } catch (Exception e) {
        System.err.println("Erreur lors de la génération du JWT VAPID: " + e.getMessage());
        e.printStackTrace();
        
        // Créer un jeton JWT statique de base pour les tests
        // Ceci est un fallback temporaire et ne devrait pas être utilisé en production
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." + 
               "eyJzdWIiOiJtYWlsdG86emVubGlmZWlubm92QGdtYWlsLmNvbSIsImV4cCI6MTc5OTk5OTk5OX0." +
               "signature_placeholder";
    }
}
}