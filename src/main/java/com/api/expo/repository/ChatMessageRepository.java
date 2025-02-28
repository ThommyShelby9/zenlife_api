// ChatMessageRepository.java (mise à jour)
package com.api.expo.repository;

import com.api.expo.models.ChatMessage;
import com.api.expo.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    
    @Query("SELECT m FROM ChatMessage m WHERE (m.sender.id = ?1 AND m.receiver.id = ?2) OR (m.sender.id = ?2 AND m.receiver.id = ?1) ORDER BY m.timestamp ASC")
    List<ChatMessage> findByConversation(String userId1, String userId2);
    
    @Query("SELECT m FROM ChatMessage m WHERE m.sender.id = ?1 AND m.receiver.id = ?2 AND m.isRead = false")
    List<ChatMessage> findUnreadMessagesFromSender(String senderId, String receiverId);
    
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.sender.id = ?1 AND m.receiver.id = ?2 AND m.isRead = false")
    long countUnreadMessagesFromSender(String senderId, String receiverId);
    
    @Query("SELECT DISTINCT u FROM User u WHERE u.id IN (SELECT DISTINCT m.sender.id FROM ChatMessage m WHERE m.receiver.id = ?1) OR u.id IN (SELECT DISTINCT m.receiver.id FROM ChatMessage m WHERE m.sender.id = ?1)")
    List<User> findContactsByUserId(String userId);
    
    @Query("SELECT m FROM ChatMessage m WHERE (m.sender.id = ?1 AND m.receiver.id = ?2) OR (m.sender.id = ?2 AND m.receiver.id = ?1) ORDER BY m.timestamp DESC")
    List<ChatMessage> findConversationOrderedByNewest(String userId1, String userId2);
    
    @Query(value = "SELECT * FROM chat_messages WHERE (sender_id = ?1 AND receiver_id = ?2) OR (sender_id = ?2 AND receiver_id = ?1) ORDER BY timestamp DESC LIMIT 1", nativeQuery = true)
    Optional<ChatMessage> findLastMessageBetweenUsers(String userId1, String userId2);
}
