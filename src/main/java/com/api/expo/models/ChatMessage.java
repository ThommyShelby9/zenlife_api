// Modèle ChatMessage corrigé
package com.api.expo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;

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
    
        private Instant timestamp;
        
        public ChatMessage() {
            this.id = UUID.randomUUID().toString();
            this.sentAt = Instant.now();
        }
    
        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
    }
    
    public Instant getTimestamp() {
        return this.timestamp;
    }
}