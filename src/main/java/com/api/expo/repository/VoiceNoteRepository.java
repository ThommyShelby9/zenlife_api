package com.api.expo.repository;

import com.api.expo.models.VoiceNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoiceNoteRepository extends JpaRepository<VoiceNote, String> {
    
    @Query("SELECT v FROM VoiceNote v WHERE v.message.id = ?1")
    Optional<VoiceNote> findByMessageId(String messageId);
}