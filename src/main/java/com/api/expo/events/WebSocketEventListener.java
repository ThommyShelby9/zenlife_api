// WebSocketEventListener.java
package com.api.expo.events;

import com.api.expo.models.User;
import com.api.expo.repository.UserRepository;
import com.api.expo.services.UserOnlineStatusService;

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
    private final UserOnlineStatusService userOnlineStatusService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        // Obtenir l'utilisateur à partir du token d'authentification
        if (headerAccessor.getUser() instanceof UsernamePasswordAuthenticationToken token) {
            String email = token.getName();
            
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // Utiliser notre service pour gérer le statut en ligne
                userOnlineStatusService.publishUserStatus(user.getId(), true);
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
                
                // Utiliser notre service pour gérer le statut en ligne
                userOnlineStatusService.publishUserStatus(user.getId(), false);
            }
        }
    }
}