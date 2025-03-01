package com.api.expo.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column
    private String link;
    
    @Column(name = "is_read", nullable = false)
    @JsonProperty("read")
    private Boolean isRead = false;
    
    @Column(name = "read_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    private Instant readAt;
    
    @Column(name = "created_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    private Instant updatedAt;
    
    public Notification() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    // Getter spécial pour fournir un mapping compatible avec le frontend
    @JsonProperty("read")
    public Boolean getRead() {
        return this.isRead;
    }
    
    // Setter spécial pour compatibilité avec le frontend
    @JsonProperty("read")
    public void setRead(Boolean read) {
        this.isRead = read;
    }
    
    // Getter qui retourne createdAt sous forme de chaîne ISO-8601 pour assurer la compatibilité
    @JsonProperty("createdAtString")
    public String getCreatedAtString() {
        return this.createdAt != null ? this.createdAt.toString() : null;
    }
}