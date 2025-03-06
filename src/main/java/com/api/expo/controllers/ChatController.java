package com.api.expo.controllers;

import com.api.expo.dto.ChatMessageDTO;
import com.api.expo.models.User;
import com.api.expo.repository.UserRepository;
import com.api.expo.services.ChatService;
import com.api.expo.services.FileService;
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
    private final FileService fileService;
    
    @PostMapping("/send/chat")
    public ResponseEntity<?> sendMessage(
            @RequestBody ChatMessageDTO messageDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("Envoi de message par: " + userDetails.getUsername());
            
            // Vérifier que l'objet receiver et son ID ne sont pas null
            if (messageDTO.getReceiver() == null || messageDTO.getReceiver().getId() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "L'ID du destinataire ne peut pas être null"
                ));
            }
            
            // Récupérer l'ID du destinataire à partir de l'objet receiver
            String receiverId = messageDTO.getReceiver().getId();
            
            // Vérifier que l'ID n'est pas vide
            if (receiverId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "L'ID du destinataire ne peut pas être vide"
                ));
            }
            
            // Récupérer le destinataire
            User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Destinataire non trouvé"));
            
            // Envoyer le message avec l'ID du message auquel on répond (si présent)
            String messageId = chatService.sendMessage(
                userDetails, 
                receiver, 
                messageDTO.getContent(),
                messageDTO.getReplyToMessageId()
            );
            
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
            @RequestParam("receiver") String receiverId,  // Garder receiver comme nom de paramètre
            @RequestParam("content") String content,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "replyToMessageId", required = false) String replyToMessageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Vérifier que l'ID du destinataire n'est pas null ou vide
            if (receiverId == null || receiverId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "L'ID du destinataire ne peut pas être null ou vide"
                ));
            }
            
            // Récupérer le destinataire
            User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Destinataire non trouvé"));
            
            // D'abord envoyer le message texte avec l'ID du message auquel on répond (si présent)
            String messageId = chatService.sendMessage(userDetails, receiver, content, replyToMessageId);
            
            // Ensuite, traiter les pièces jointes
            List<Map<String, Object>> attachmentDetails = new ArrayList<>();
            
            for (MultipartFile file : files) {
                Map<String, Object> attachmentInfo;
                
                // Traiter différemment les notes vocales 
                if (file.getContentType() != null && file.getContentType().startsWith("audio/") && 
                    file.getOriginalFilename() != null && file.getOriginalFilename().contains("voice-note")) {
                    
                    // Estimer la durée (à remplacer par la durée réelle si disponible)
                    double estimatedDuration = Math.max(1.0, file.getSize() / 16000.0); // Estimation grossière
                    
                    // Sauvegarder comme note vocale
                    attachmentInfo = fileService.saveVoiceNote(file, messageId, estimatedDuration);
                } else {
                    // Sauvegarder comme pièce jointe normale
                    attachmentInfo = fileService.storeFile(file, messageId);
                }
                
                attachmentDetails.add(attachmentInfo);
            }
            
            response.put("status", "success");
            response.put("message", "Message avec pièces jointes envoyé avec succès");
            response.put("messageId", messageId);
            response.put("attachments", attachmentDetails);
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
    // Nouveau endpoint spécifique pour les notes vocales
    @PostMapping("/send/chat/voice-note")
    public ResponseEntity<?> sendVoiceNote(
            @RequestParam("receiver") String receiverId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("durationSeconds") Double durationSeconds,
            @RequestParam(value = "replyToMessageId", required = false) String replyToMessageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Récupérer le destinataire
            User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Destinataire non trouvé"));
            
            // Créer un message pour la note vocale avec l'ID du message auquel on répond (si présent)
            String messageId = chatService.sendMessage(userDetails, receiver, "[NOTE_VOCALE]", replyToMessageId);
            
            // Enregistrer la note vocale avec la durée fournie
            Map<String, Object> voiceNoteInfo = fileService.saveVoiceNote(file, messageId, durationSeconds);
            
            response.put("status", "success");
            response.put("message", "Note vocale envoyée avec succès");
            response.put("messageId", messageId);
            response.put("voiceNote", voiceNoteInfo);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Erreur lors de l'envoi de la note vocale: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors de l'envoi de la note vocale");
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