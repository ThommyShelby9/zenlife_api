// FileAttachmentRepository.java
package com.api.expo.repository;

import com.api.expo.models.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, String> {
    List<FileAttachment> findByMessageId(String messageId);
}