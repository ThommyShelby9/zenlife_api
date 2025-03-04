package com.api.expo.services;

import com.api.expo.models.Notification;
import com.api.expo.models.User;
import com.api.expo.repository.NotificationRepository;
import com.api.expo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final FCMPushService fcmPushService; // Nouvelle dépendance pour FCM
    
    public Notification createNotification(UserDetails userDetails, String type, String content, String link) {
        // Récupérer l'objet User à partir du UserDetails
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        return createSystemNotification(user, type, content, link);
    }
    
    public Notification createSystemNotification(User user, String type, String content, String link) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setContent(content);
        notification.setLink(link);
        notification.setCreatedAt(Instant.now());
        notification.setRead(false);
        
        Notification savedNotification = notificationRepository.save(notification);
        
        // Envoyer à l'utilisateur spécifique via WebSocket (pour les utilisateurs connectés)
        messagingTemplate.convertAndSendToUser(
            user.getId(),
            "/queue/notifications",
            savedNotification
        );
        
        // Envoyer également via FCM (pour les utilisateurs non connectés)
        sendPushNotification(user, type, content, link);
        
        return savedNotification;
    }
    
    /**
     * Méthode privée pour envoyer une notification push via FCM
     */
    private void sendPushNotification(User user, String type, String content, String link) {
        // Déterminer le titre et l'icône en fonction du type de notification
        String title = getTitleForNotificationType(type);
        String icon = getIconForNotificationType(type);
        
        // Envoyer la notification via FCM
        fcmPushService.sendNotification(
            user,
            title,
            content,
            icon,
            type, // Utiliser le type comme tag
            link  // URL de redirection
        );
    }
    
    /**
     * Déterminer le titre approprié en fonction du type de notification
     */
    private String getTitleForNotificationType(String type) {
        switch (type) {
            case "WATER_REMINDER":
                return "Rappel d'hydratation";
            case "WATER_PROGRESS":
                return "Progression d'hydratation";
            case "POSITIVE_THOUGHT":
                return "Pensée positive";
            case "TASK_REMINDER":
                return "Rappel de tâche";
            case "FRIEND_REQUEST":
                return "Demande d'ami";
            case "FRIEND_ACCEPTED":
                return "Demande acceptée";
            default:
                return "ZenLife";
        }
    }
    
    /**
     * Déterminer l'icône appropriée en fonction du type de notification
     */
    private String getIconForNotificationType(String type) {
        switch (type) {
            case "WATER_REMINDER":
            case "WATER_PROGRESS":
                return "/img/water-icon.png";
            case "POSITIVE_THOUGHT":
                return "/img/positive-thought-icon.png";
            case "TASK_REMINDER":
                return "/img/task-icon.png";
            case "FRIEND_REQUEST":
            case "FRIEND_ACCEPTED":
                return "/img/friend-icon.png";
            default:
                return "/img/logo.png";
        }
    }
    
    public List<Notification> getUserNotifications(UserDetails userDetails) {
        // Récupérer l'objet User à partir du UserDetails
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
    
    public List<Notification> getUnreadNotifications(UserDetails userDetails) {
        // Récupérer l'objet User à partir du UserDetails
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId());
    }
    
    public long countUnreadNotifications(UserDetails userDetails) {
        // Récupérer l'objet User à partir du UserDetails
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }
    
    public void markAsRead(String notificationId, UserDetails userDetails) {
        // Récupérer l'objet User à partir du UserDetails
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification non trouvée"));
            
        if (notification.getUser().getId().equals(user.getId())) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }
    
    public void markAllAsRead(UserDetails userDetails) {
        // Récupérer l'objet User à partir du UserDetails
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId());
        
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        }
        
        if (!unreadNotifications.isEmpty()) {
            notificationRepository.saveAll(unreadNotifications);
        }
    }
    
    public void deleteNotification(String notificationId, UserDetails userDetails) {
        // Récupérer l'objet User à partir du UserDetails
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification non trouvée"));
            
        if (notification.getUser().getId().equals(user.getId())) {
            notificationRepository.delete(notification);
        }
    }
    
    /**
     * Envoyer une notification de rappel d'eau
     */
    public Notification sendWaterReminderNotification(User user, String message) {
        return createSystemNotification(
            user,
            "WATER_REMINDER",
            message != null ? message : "Rappel: C'est l'heure de boire de l'eau! 💦",
            "/water-tracker"
        );
    }
    
    /**
     * Créer une notification de progression d'hydratation
     */
    public Notification createWaterProgressNotification(User user, int percentage) {
        String message;
        
        if (percentage >= 100) {
            message = "Félicitations! Vous avez atteint votre objectif d'hydratation aujourd'hui! 🎉💧";
        } else if (percentage >= 75) {
            message = "Vous avez atteint " + percentage + "% de votre objectif d'hydratation journalier! 💧";
        } else if (percentage >= 50) {
            message = "Vous êtes à mi-chemin de votre objectif d'hydratation journalier. Continuez! 💧";
        } else {
            message = "N'oubliez pas de boire de l'eau régulièrement! Vous êtes à " + percentage + "% de votre objectif. 💧";
        }
        
        return createSystemNotification(
            user,
            "WATER_PROGRESS",
            message,
            "/water-tracker"
        );
    }

    public Notification createSystemNotification(User user, String type, String content, String link, Map<String, Object> additionalData) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setContent(content);
        notification.setLink(link);
        notification.setCreatedAt(Instant.now());
        notification.setRead(false);
        
        Notification savedNotification = notificationRepository.save(notification);
        
        // Créer le message complet avec les données additionnelles
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("id", savedNotification.getId());
        messageData.put("type", savedNotification.getType());
        messageData.put("content", savedNotification.getContent());
        messageData.put("link", savedNotification.getLink());
        messageData.put("createdAt", savedNotification.getCreatedAt());
        messageData.put("read", savedNotification.getRead());
        
        // Ajouter les données additionnelles
        if (additionalData != null) {
            messageData.putAll(additionalData);
        }
        
        // Envoyer à l'utilisateur spécifique avec toutes les données via WebSocket
        messagingTemplate.convertAndSendToUser(
            user.getId(),
            "/queue/notifications",
            messageData
        );
        
        // Envoyer également via FCM (pour les notifications push)
        sendPushNotification(user, type, content, link);
        
        return savedNotification;
    }
}