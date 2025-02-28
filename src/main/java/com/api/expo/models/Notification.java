// Modèle Notification corrigé
package com.api.expo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "NOTIFICATIONS")
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String type;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    private String link;
    
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;
    
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant createdAt;
    
    @Column(name = "read_at", columnDefinition = "TIMESTAMP")
    private Instant readAt;
    
    public Notification() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }
}