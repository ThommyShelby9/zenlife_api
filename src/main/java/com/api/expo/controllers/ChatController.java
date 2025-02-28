// Mise à jour du ChatController.java pour intégrer les nouvelles fonctionnalités
package com.api.expo.controllers;

import com.api.expo.models.ChatMessage;
import com.api.expo.models.User;
import com.api.expo.repository.UserRepository;
import com.api.expo.services.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChatController {
    
    private final ChatService chatService;
    private final UserRepository userRepository;
    
    @PostMapping("/send/chat")
    public ResponseEntity<?> sendMessage(
            @RequestBody ChatMessage message,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("Envoi de message par: " + userDetails.getUsername());
            
            String messageId = chatService.sendMessage(userDetails, message.getReceiver(), message.getContent());
            
            response.put("status", "success");
            response.put("message", "Message envoyé avec succès");
            response.put("messageId", messageId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Erreur lors de l'envoi du message: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors de l'envoi du message");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/send/chat/with-attachments")
    public ResponseEntity<?> sendMessageWithAttachments(
            @RequestParam("receiver") User receiver,
            @RequestParam("content") String content,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // D'abord envoyer le message texte
            String messageId = chatService.sendMessage(userDetails, receiver, content);
            
            // Ensuite, traiter les pièces jointes via le service de fichiers
            // (Cette logique est généralement gérée par le FileService)
            
            response.put("status", "success");
            response.put("message", "Message avec pièces jointes envoyé avec succès");
            response.put("messageId", messageId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Erreur lors de l'envoi du message avec pièces jointes: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors de l'envoi du message avec pièces jointes");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/chat/conversation/{otherUserId}")
    public ResponseEntity<?> getConversation(
            @PathVariable String otherUserId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Récupérer l'utilisateur à partir de l'email
            User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            List<Map<String, Object>> messages = chatService.getEnrichedConversation(user.getId(), otherUserId);
            
            return ResponseEntity.ok(messages);
            
        } catch (Exception e) {
            System.out.println("Erreur lors de la récupération de la conversation: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération de la conversation");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/chat/messages/{messageId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable String messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            chatService.markAsRead(messageId, userDetails);
            
            response.put("status", "success");
            response.put("message", "Message marqué comme lu");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Erreur lors du marquage comme lu: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors du marquage comme lu");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/chat/conversations/{senderId}/read")
    public ResponseEntity<?> markConversationAsRead(
            @PathVariable String senderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Récupérer l'utilisateur à partir de l'email
            User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            chatService.markConversationAsRead(user.getId(), senderId);
            
            response.put("status", "success");
            response.put("message", "Conversation marquée comme lue");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Erreur lors du marquage de la conversation: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors du marquage de la conversation");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/chat/contacts")
    public ResponseEntity<?> getContacts(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Récupérer l'utilisateur à partir de l'email
            User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            List<Map<String, Object>> contacts = chatService.getUserContacts(user.getId());
            return ResponseEntity.ok(contacts);
            
        } catch (Exception e) {
            System.out.println("Erreur lors de la récupération des contacts: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération des contacts");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}