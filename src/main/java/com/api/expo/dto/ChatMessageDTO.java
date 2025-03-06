package com.api.expo.dto;

import lombok.Data;

@Data
public class ChatMessageDTO {
    private ReceiverDTO receiver;  // Objet receiver au lieu de receiverId
    private String content;
    private String replyToMessageId;  // Peut être null pour les messages normaux
    
    // Classe interne pour représenter le destinataire
    @Data
    public static class ReceiverDTO {
        private String id;
    }
}