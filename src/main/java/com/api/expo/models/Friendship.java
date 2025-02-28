// Friendship.java
package com.api.expo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "FRIENDSHIPS")
public class Friendship {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;
    
    @ManyToOne
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;
    
    @Column(nullable = false)
    private String status; // "PENDING", "ACCEPTED", "REJECTED", "BLOCKED"
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "accepted_at")
    private Instant acceptedAt;
    
    public Friendship() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}