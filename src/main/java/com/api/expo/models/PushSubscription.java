// PushSubscription.java
package com.api.expo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "PUSH_SUBSCRIPTIONS")
public class PushSubscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint;
    
    @Column(name = "p256dh", nullable = false, length = 255)
    private String p256dh;
    
    @Column(name = "auth", nullable = false, length = 255)
    private String auth;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    public PushSubscription() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}