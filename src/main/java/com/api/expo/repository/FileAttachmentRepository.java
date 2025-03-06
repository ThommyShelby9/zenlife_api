package com.api.expo.repository;

import com.api.expo.models.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, String> {

    @Query("SELECT f FROM FileAttachment f WHERE f.message.id = ?1")
    List<FileAttachment> findByMessageId(String messageId);
    
    Optional<FileAttachment> findByStoragePath(String storagePath);
    
    Optional<FileAttachment> findByFilename(String filename);
}