package com.api.expo.services;

import com.api.expo.models.ChatMessage;
import com.api.expo.models.FileAttachment;
import com.api.expo.models.User;
import com.api.expo.models.VoiceNote;
import com.api.expo.repository.ChatMessageRepository;
import com.api.expo.repository.FileAttachmentRepository;
import com.api.expo.repository.FriendshipRepository;
import com.api.expo.repository.UserRepository;
import com.api.expo.repository.VoiceNoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final VoiceNoteRepository voiceNoteRepository;
    private final NotificationService notificationService;
    private final SimpMessageSendingOperations messagingTemplate;
    private final UserOnlineStatusService userOnlineStatusService;
    private final FriendshipRepository friendshipRepository;
    
    /**
     * Envoie un message avec possibilité de répondre à un autre message
     */
    public String sendMessage(UserDetails userDetails, User receiver, String content, String replyToMessageId) {
        // Récupérer l'expéditeur
        User sender = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur expéditeur non trouvé"));
        
        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setTimestamp(Instant.now());
        message.setSentAt(Instant.now());
        message.setRead(false);
        
        // Ajouter la référence au message auquel on répond si présent
        if (replyToMessageId != null && !replyToMessageId.isEmpty()) {
            ChatMessage originalMessage = chatMessageRepository.findById(replyToMessageId)
                .orElse(null);
            message.setReplyToMessage(originalMessage);
        }
        
        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // Envoyer au destinataire spécifique via WebSocket
        messagingTemplate.convertAndSendToUser(
            receiver.getId(),
            "/queue/messages",
            enrichMessageWithAttachments(savedMessage)
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
    
    /**
     * Méthode de compatibilité avec l'ancien code
     */
    public String sendMessage(UserDetails userDetails, User receiver, String content) {
        return sendMessage(userDetails, receiver, content, null);
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
        // Code existant inchangé...
        
        // 1. Récupérer tous les utilisateurs avec qui l'utilisateur actuel a échangé des messages
        List<User> messageContacts = chatMessageRepository.findContactsByUserId(userId);
        
        // 2. Récupérer tous les amis de l'utilisateur (même sans messages)
        List<User> friends = friendshipRepository.findAcceptedFriendshipsByUserId(userId).stream()
            .map(friendship -> {
                if (friendship.getRequester().getId().equals(userId)) {
                    return friendship.getAddressee();
                } else {
                    return friendship.getRequester();
                }
            })
            .collect(Collectors.toList());
        
        // 3. Combiner les deux listes et éliminer les doublons
        Set<String> contactIds = new HashSet<>();
        List<User> allContacts = new ArrayList<>();
        
        // Ajouter d'abord tous les contacts avec qui il y a eu des messages
        for (User contact : messageContacts) {
            if (!contactIds.contains(contact.getId())) {
                contactIds.add(contact.getId());
                allContacts.add(contact);
            }
        }
        
        // Puis ajouter les amis qui ne sont pas encore dans la liste
        for (User friend : friends) {
            if (!contactIds.contains(friend.getId())) {
                contactIds.add(friend.getId());
                allContacts.add(friend);
            }
        }
        
        // 4. Obtenir les détails pour chaque contact
        List<Map<String, Object>> contactsWithDetails = new ArrayList<>();
        
        for (User contact : allContacts) {
            Map<String, Object> contactDetails = new HashMap<>();
            contactDetails.put("id", contact.getId());
            contactDetails.put("fullName", contact.getFullName());
            contactDetails.put("username", contact.getUsername());
            contactDetails.put("email", contact.getEmail());
            
            // Utiliser profilePictureUrl pour Cloudinary
            contactDetails.put("profilePictureUrl", contact.getProfilePictureUrl());
            
            // Vérifier le statut en ligne
            boolean isOnline = userOnlineStatusService.isUserOnline(contact.getId());
            contactDetails.put("online", isOnline);
            
            // Obtenir le dernier message échangé (s'il existe)
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
    
    /**
     * Enrichit un message avec ses pièces jointes et les informations de réponse
     */
    public Map<String, Object> enrichMessageWithAttachments(ChatMessage message) {
        Map<String, Object> enrichedMessage = new HashMap<>();
        
        // Informations sur l'expéditeur
        enrichedMessage.put("sender", Map.of(
            "id", message.getSender().getId(),
            "fullName", message.getSender().getFullName(),
            "profilePictureUrl", message.getSender().getProfilePictureUrl() != null ? 
                message.getSender().getProfilePictureUrl() : ""
        ));
        
        // Informations sur le destinataire
        enrichedMessage.put("receiver", Map.of(
            "id", message.getReceiver().getId()
        ));
        
        // Informations de base sur le message
        enrichedMessage.put("id", message.getId());
        enrichedMessage.put("content", message.getContent());
        enrichedMessage.put("timestamp", message.getTimestamp().toString());
        enrichedMessage.put("read", message.isRead());
        enrichedMessage.put("readAt", message.getReadAt() != null ? message.getReadAt().toString() : null);
        
        // Ajouter les informations sur le message auquel on répond (si présent)
        if (message.getReplyToMessage() != null) {
            Map<String, Object> replyInfo = new HashMap<>();
            replyInfo.put("messageId", message.getReplyToMessage().getId());
            replyInfo.put("content", message.getReplyToMessage().getContent());
            replyInfo.put("senderId", message.getReplyToMessage().getSender().getId());
            replyInfo.put("senderName", message.getReplyToMessage().getSender().getFullName());
            enrichedMessage.put("replyTo", replyInfo);
        }
        
        // Ajouter les pièces jointes
        List<Map<String, Object>> attachmentDetails = new ArrayList<>();
        
        // Récupérer toutes les pièces jointes
        List<FileAttachment> attachments = fileAttachmentRepository.findByMessageId(message.getId());
        for (FileAttachment attachment : attachments) {
            Map<String, Object> attachmentInfo = new HashMap<>();
            attachmentInfo.put("id", attachment.getId());
            attachmentInfo.put("filename", attachment.getFilename());
            attachmentInfo.put("contentType", attachment.getContentType());
            attachmentInfo.put("size", attachment.getFileSize());
            
            // Utiliser directement l'URL Cloudinary si disponible
            if (attachment.getStoragePath().startsWith("http")) {
                attachmentInfo.put("url", attachment.getStoragePath());
            } else {
                // Sinon, construire une URL locale
                attachmentInfo.put("url", "/api/files/view/" + attachment.getStoragePath());
            }
            
            // Si c'est un fichier audio, vérifier s'il s'agit d'une note vocale
            if (attachment.getContentType().startsWith("audio/")) {
                Optional<VoiceNote> voiceNote = voiceNoteRepository.findByMessageId(message.getId());
                if (voiceNote.isPresent()) {
                    attachmentInfo.put("durationSeconds", voiceNote.get().getDurationSeconds());
                    attachmentInfo.put("isVoiceNote", true);
                }
            }
            
            attachmentDetails.add(attachmentInfo);
        }
        
        enrichedMessage.put("attachments", attachmentDetails);
        
        return enrichedMessage;
    }
    
    public List<Map<String, Object>> getEnrichedConversation(String user1Id, String user2Id) {
        List<ChatMessage> messages = chatMessageRepository.findByConversation(user1Id, user2Id);
        
        List<Map<String, Object>> enrichedMessages = new ArrayList<>();
        for (ChatMessage message : messages) {
            enrichedMessages.add(enrichMessageWithAttachments(message));
        }
        
        return enrichedMessages;
    }
}