// Mise à jour du ChatService.java pour ajouter des fonctionnalités
package com.api.expo.services;

import com.api.expo.models.ChatMessage;
import com.api.expo.models.FileAttachment;
import com.api.expo.models.User;
import com.api.expo.models.VoiceNote;
import com.api.expo.repository.ChatMessageRepository;
import com.api.expo.repository.FileAttachmentRepository;
import com.api.expo.repository.UserRepository;
import com.api.expo.repository.VoiceNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final VoiceNoteRepository voiceNoteRepository;
    private final NotificationService notificationService;
    private final SimpMessageSendingOperations messagingTemplate;
    
    public String sendMessage(UserDetails userDetails, User receiverObj, String content) {
        // Récupérer l'objet User à partir du UserDetails
        User sender = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur expéditeur non trouvé"));
            
        // Utiliser l'ID du receiver pour récupérer l'objet User complet depuis la base de données
        String receiverId = receiverObj.getId();
        User receiver = userRepository.findById(receiverId)
            .orElseThrow(() -> new RuntimeException("Utilisateur destinataire non trouvé"));
            
        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setTimestamp(Instant.now());
        message.setSentAt(Instant.now());
        message.setRead(false);
        
        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // Envoyer au destinataire spécifique via WebSocket
        messagingTemplate.convertAndSendToUser(
            receiver.getId(),
            "/queue/messages",
            savedMessage
        );
        
        // Créer une notification pour le destinataire
        notificationService.createSystemNotification(
            receiver,
            "NEW_MESSAGE",
            "Nouveau message de " + sender.getFullName(),
            "/chat/" + sender.getId()
        );
        
        return savedMessage.getId();
    }
    
    public String sendMessageWithAttachments(UserDetails userDetails, User receiver, String content, List<FileAttachment> attachments) {
        // D'abord envoyer le message
        String messageId = sendMessage(userDetails, receiver, content);
        
        // Puis associer les pièces jointes au message
        // Cette étape est généralement gérée par le service de fichiers
        
        return messageId;
    }
    
    public String sendVoiceNote(UserDetails userDetails, User receiver, VoiceNote voiceNote) {
        // Récupérer l'objet User à partir du UserDetails
        User sender = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur expéditeur non trouvé"));
            
        // Créer un message avec un contenu spécial pour les notes vocales
        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent("[NOTE_VOCALE]");
        message.setTimestamp(Instant.now());
        message.setSentAt(Instant.now());
        message.setRead(false);
        
        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // Associer la note vocale au message
        // Cette étape est généralement gérée par le service de fichiers
        
        // Envoyer au destinataire spécifique
        messagingTemplate.convertAndSendToUser(
            receiver.getId(),
            "/queue/messages",
            savedMessage
        );
        
        // Créer une notification pour le destinataire
        notificationService.createSystemNotification(
            receiver,
            "NEW_VOICE_NOTE",
            "Nouvelle note vocale de " + sender.getFullName(),
            "/chat/" + sender.getId()
        );
        
        return savedMessage.getId();
    }
    
    public List<ChatMessage> getConversation(String user1Id, String user2Id) {
        return chatMessageRepository.findByConversation(user1Id, user2Id);
    }
    
    public void markAsRead(String messageId, UserDetails userDetails) {
        ChatMessage message = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message non trouvé"));
        
        // Récupérer l'objet User à partir du UserDetails
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        if (message.getReceiver().getId().equals(currentUser.getId())) {
            message.setRead(true);
            message.setReadAt(Instant.now());
            chatMessageRepository.save(message);
            
            // Informer l'expéditeur que le message a été lu via WebSocket
            messagingTemplate.convertAndSendToUser(
                message.getSender().getId(),
                "/queue/read-receipts",
                Map.of("messageId", messageId, "readAt", message.getReadAt())
            );
        }
    }
    
    public void markConversationAsRead(String receiverId, String senderId) {
        List<ChatMessage> unreadMessages = chatMessageRepository.findUnreadMessagesFromSender(senderId, receiverId);
        
        for (ChatMessage message : unreadMessages) {
            message.setRead(true);
            message.setReadAt(Instant.now());
        }
        
        if (!unreadMessages.isEmpty()) {
            chatMessageRepository.saveAll(unreadMessages);
            
            // Informer l'expéditeur que tous les messages ont été lus
            messagingTemplate.convertAndSendToUser(
                senderId,
                "/queue/read-receipts",
                Map.of("conversationRead", true, "userId", receiverId)
            );
        }
    }
    
    public List<Map<String, Object>> getUserContacts(String userId) {
        // Récupérer tous les utilisateurs avec qui l'utilisateur actuel a échangé des messages
        List<User> contacts = chatMessageRepository.findContactsByUserId(userId);
        
        List<Map<String, Object>> contactsWithDetails = new ArrayList<>();
        
        for (User contact : contacts) {
            Map<String, Object> contactDetails = new HashMap<>();
            contactDetails.put("id", contact.getId());
            contactDetails.put("name", contact.getFullName());
            contactDetails.put("username", contact.getUsername());
            contactDetails.put("email", contact.getEmail());
            contactDetails.put("profilePictureUrl", contact.getProfilePictureUrl());
            
            // Obtenir le dernier message échangé
            Optional<ChatMessage> lastMessage = chatMessageRepository.findLastMessageBetweenUsers(userId, contact.getId());
            
            if (lastMessage.isPresent()) {
                ChatMessage message = lastMessage.get();
                contactDetails.put("lastMessage", message.getContent());
                contactDetails.put("lastMessageTime", message.getTimestamp());
                contactDetails.put("isLastMessageFromMe", message.getSender().getId().equals(userId));
                
                // Vérifier si le message est une note vocale ou a des pièces jointes
                if ("[NOTE_VOCALE]".equals(message.getContent())) {
                    contactDetails.put("isVoiceNote", true);
                    
                    // Obtenir les détails de la note vocale
                    Optional<VoiceNote> voiceNote = voiceNoteRepository.findByMessageId(message.getId());
                    if (voiceNote.isPresent()) {
                        contactDetails.put("voiceNoteDuration", voiceNote.get().getDurationSeconds());
                    }
                } else {
                    contactDetails.put("isVoiceNote", false);
                }
                
                // Vérifier s'il y a des pièces jointes
                List<FileAttachment> attachments = fileAttachmentRepository.findByMessageId(message.getId());
                contactDetails.put("hasAttachments", !attachments.isEmpty());
                contactDetails.put("attachmentsCount", attachments.size());
                
                // Compter les messages non lus de ce contact
                long unreadCount = chatMessageRepository.countUnreadMessagesFromSender(contact.getId(), userId);
                contactDetails.put("unreadCount", unreadCount);
            } else {
                contactDetails.put("lastMessage", "");
                contactDetails.put("lastMessageTime", null);
                contactDetails.put("isLastMessageFromMe", false);
                contactDetails.put("isVoiceNote", false);
                contactDetails.put("hasAttachments", false);
                contactDetails.put("attachmentsCount", 0);
                contactDetails.put("unreadCount", 0);
            }
            
            contactsWithDetails.add(contactDetails);
        }
        
        // Trier par dernier message (plus récent en premier)
        contactsWithDetails.sort((c1, c2) -> {
            Instant time1 = (Instant) c1.get("lastMessageTime");
            Instant time2 = (Instant) c2.get("lastMessageTime");
            
            if (time1 == null && time2 == null) return 0;
            if (time1 == null) return 1;
            if (time2 == null) return -1;
            
            return time2.compareTo(time1);
        });
        
        return contactsWithDetails;
    }
    
    public Map<String, Object> enrichMessageWithAttachments(ChatMessage message) {
        Map<String, Object> enrichedMessage = new HashMap<>();
        
        // Copier les propriétés du message
        enrichedMessage.put("id", message.getId());
        enrichedMessage.put("senderId", message.getSender().getId());
        enrichedMessage.put("senderName", message.getSender().getFullName());
        enrichedMessage.put("senderUsername", message.getSender().getUsername());
        enrichedMessage.put("senderProfilePicture", message.getSender().getProfilePictureUrl());
        enrichedMessage.put("receiverId", message.getReceiver().getId());
        enrichedMessage.put("content", message.getContent());
        enrichedMessage.put("timestamp", message.getTimestamp());
        enrichedMessage.put("isRead", message.isRead());
        enrichedMessage.put("readAt", message.getReadAt());
        
        // Vérifier si c'est une note vocale
        if ("[NOTE_VOCALE]".equals(message.getContent())) {
            enrichedMessage.put("isVoiceNote", true);
            
            Optional<VoiceNote> voiceNote = voiceNoteRepository.findByMessageId(message.getId());
            if (voiceNote.isPresent()) {
                Map<String, Object> voiceNoteMap = new HashMap<>();
                voiceNoteMap.put("durationSeconds", voiceNote.get().getDurationSeconds());
                voiceNoteMap.put("url", "/api/files/voice-notes/" + voiceNote.get().getStoragePath());
                enrichedMessage.put("voiceNote", voiceNoteMap);
            }
        } else {
            enrichedMessage.put("isVoiceNote", false);
        }
        
        // Ajouter les pièces jointes
        List<FileAttachment> attachments = fileAttachmentRepository.findByMessageId(message.getId());
        if (!attachments.isEmpty()) {
            List<Map<String, Object>> attachmentDetails = attachments.stream()
                .map(attachment -> {
                    Map<String, Object> attachMap = new HashMap<>();
                    attachMap.put("id", attachment.getId());
                    attachMap.put("filename", attachment.getFilename());
                    attachMap.put("contentType", attachment.getContentType());
                    attachMap.put("size", attachment.getFileSize());
                    attachMap.put("url", "/api/files/attachments/" + attachment.getStoragePath());
                    return attachMap;
                })
                .collect(Collectors.toList());
                
            enrichedMessage.put("attachments", attachmentDetails);
        } else {
            enrichedMessage.put("attachments", Collections.emptyList());
        }
        
        return enrichedMessage;
    }
    
    public List<Map<String, Object>> getEnrichedConversation(String user1Id, String user2Id) {
        List<ChatMessage> messages = chatMessageRepository.findByConversation(user1Id, user2Id);
        
        return messages.stream()
            .map(this::enrichMessageWithAttachments)
            .collect(Collectors.toList());
    }
}