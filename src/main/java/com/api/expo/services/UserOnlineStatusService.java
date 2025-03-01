package com.api.expo.services;

import com.api.expo.models.User;
import com.api.expo.models.UserOnlineStatus;
import com.api.expo.repository.UserOnlineStatusRepository;
import com.api.expo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserOnlineStatusService {

    private final SimpMessageSendingOperations messagingTemplate;
    private final UserRepository userRepository;
    private final UserOnlineStatusRepository userOnlineStatusRepository;
    
    /**
     * Marque un utilisateur comme en ligne
     */
    public void setUserOnline(String userId) {
        UserOnlineStatus status = userOnlineStatusRepository.findById(userId)
            .orElse(new UserOnlineStatus(userId));
        status.setOnline(true);
        status.setLastActivity(Instant.now());
        userOnlineStatusRepository.save(status);
        
        // Notifier les autres utilisateurs
        publishUserStatus(userId, true);
    }
    
    /**
     * Marque un utilisateur comme hors ligne
     */
    public void setUserOffline(String userId) {
        userOnlineStatusRepository.findById(userId).ifPresent(status -> {
            status.setOnline(false);
            userOnlineStatusRepository.save(status);
            
            // Notifier les autres utilisateurs
            publishUserStatus(userId, false);
        });
    }
    
    /**
     * Vérifie si un utilisateur est en ligne
     */
    public boolean isUserOnline(String userId) {
        return userOnlineStatusRepository.findById(userId)
            .map(UserOnlineStatus::isOnline)
            .orElse(false);
    }
    
    /**
     * Obtient le statut en ligne pour une liste d'utilisateurs
     */
    public Map<String, Boolean> getOnlineStatusForUsers(List<String> userIds) {
        List<UserOnlineStatus> onlineStatuses = userOnlineStatusRepository.findOnlineUsersByUserIds(userIds);
        Map<String, Boolean> result = new HashMap<>();
        
        // Initialiser tous les utilisateurs comme hors ligne
        for (String userId : userIds) {
            result.put(userId, false);
        }
        
        // Mettre à jour les utilisateurs qui sont en ligne
        for (UserOnlineStatus status : onlineStatuses) {
            result.put(status.getUserId(), true);
        }
        
        return result;
    }
    
    /**
     * Publie le statut en ligne d'un utilisateur via WebSocket
     */
    public void publishUserStatus(String userId, boolean online) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        
        Map<String, Object> status = new HashMap<>();
        status.put("userId", userId);
        status.put("online", online);
        status.put("fullName", user.getFullName());
        status.put("timestamp", Instant.now().toString());
        
        messagingTemplate.convertAndSend("/topic/user-status", status);
    }
    
    /**
     * Obtient tous les utilisateurs en ligne
     */
    public List<String> getAllOnlineUsers() {
        return userOnlineStatusRepository.findByOnlineTrue()
            .stream()
            .map(UserOnlineStatus::getUserId)
            .collect(Collectors.toList());
    }
    
    /**
     * Tâche planifiée pour mettre à jour le statut des utilisateurs inactifs
     */
    @Scheduled(fixedRate = 60000) // Exécuter toutes les minutes
    public void updateInactiveUsers() {
        Instant threshold = Instant.now().minus(5, ChronoUnit.MINUTES);
        List<UserOnlineStatus> activeUsers = userOnlineStatusRepository.findByOnlineTrue();
        
        for (UserOnlineStatus status : activeUsers) {
            if (status.getLastActivity().isBefore(threshold)) {
                status.setOnline(false);
                userOnlineStatusRepository.save(status);
                
                // Notifier les autres utilisateurs
                publishUserStatus(status.getUserId(), false);
            }
        }
    }
}