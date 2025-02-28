// VoiceNote.java
package com.api.expo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "VOICE_NOTES")
public class VoiceNote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    
    @OneToOne
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;
    
    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;
    
    @Column(name = "storage_path", nullable = false)
    private String storagePath;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    public VoiceNote() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }
}
