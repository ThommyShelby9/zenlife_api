package com.api.expo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "CHAT_MESSAGES")
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;
    
    @Column(name = "sent_at", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant sentAt;
    
    @Column(name = "read_at", columnDefinition = "TIMESTAMP")
    private Instant readAt;
    
    @Column(nullable = false, columnDefinition = "TIMESTAMP")
    private Instant timestamp;
    
    // Nouveau champ pour répondre à un message
    @ManyToOne
    @JoinColumn(name = "reply_to_message_id")
    private ChatMessage replyToMessage;
    
    public ChatMessage() {
        this.id = UUID.randomUUID().toString();
        this.sentAt = Instant.now();
        this.timestamp = Instant.now();
    }
}