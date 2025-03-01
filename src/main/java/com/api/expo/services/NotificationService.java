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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    
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
        
        // Envoyer à l'utilisateur spécifique
        messagingTemplate.convertAndSendToUser(
            user.getId(),
            "/queue/notifications",
            savedNotification
        );
        
        return savedNotification;
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
}