// FileAttachment.java
package com.api.expo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "FILE_ATTACHMENTS")
public class FileAttachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "message_id")
    private ChatMessage message;
    
    @Column(nullable = false)
    private String filename;
    
    @Column(nullable = false)
    private String contentType;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "storage_path", nullable = false)
    private String storagePath;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    public FileAttachment() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }
}