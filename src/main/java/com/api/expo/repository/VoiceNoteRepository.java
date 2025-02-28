// VoiceNoteRepository.java
package com.api.expo.repository;

import com.api.expo.models.VoiceNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoiceNoteRepository extends JpaRepository<VoiceNote, String> {
    Optional<VoiceNote> findByMessageId(String messageId);
}