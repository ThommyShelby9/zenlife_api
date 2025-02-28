// WebSocketEventListener.java
package com.api.expo.events;

import com.api.expo.models.User;
import com.api.expo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final UserRepository userRepository;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        // Obtenir l'utilisateur à partir du token d'authentification
        if (headerAccessor.getUser() instanceof UsernamePasswordAuthenticationToken token) {
            String email = token.getName();
            
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // Envoyer une notification de connexion à tous les autres utilisateurs
                Map<String, Object> connectionStatus = new HashMap<>();
                connectionStatus.put("userId", user.getId());
                connectionStatus.put("status", "ONLINE");
                
                messagingTemplate.convertAndSend("/topic/user-status", connectionStatus);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        // Obtenir l'utilisateur à partir du token d'authentification
        if (headerAccessor.getUser() instanceof UsernamePasswordAuthenticationToken token) {
            String email = token.getName();
            
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // Envoyer une notification de déconnexion à tous les autres utilisateurs
                Map<String, Object> connectionStatus = new HashMap<>();
                connectionStatus.put("userId", user.getId());
                connectionStatus.put("status", "OFFLINE");
                
                messagingTemplate.convertAndSend("/topic/user-status", connectionStatus);
            }
        }
    }
}