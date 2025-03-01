// FriendshipRepository.java
package com.api.expo.repository;

import com.api.expo.models.Friendship;
import com.api.expo.models.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, String> {
    @Query("SELECT f FROM Friendship f WHERE (f.requester.id = ?1 AND f.addressee.id = ?2) OR (f.requester.id = ?2 AND f.addressee.id = ?1)")
    Optional<Friendship> findByUserIds(String userId1, String userId2);
    
    @Query("SELECT f FROM Friendship f WHERE (f.requester.id = ?1 OR f.addressee.id = ?1) AND f.status = 'ACCEPTED'")
    List<Friendship> findAcceptedFriendshipsByUserId(String userId);
    
    @Query("SELECT f FROM Friendship f WHERE f.addressee.id = ?1 AND f.status = 'PENDING'")
    List<Friendship> findPendingFriendRequestsForUser(String userId);
    
    @Query("SELECT f FROM Friendship f WHERE f.requester.id = ?1 AND f.status = 'PENDING'")
    List<Friendship> findSentFriendRequestsByUser(String userId);

    @Query("SELECT f FROM Friendship f WHERE f.requester.id = ?1 AND f.status = ?2")
List<Friendship> findByRequesterIdAndStatus(String userId, String status);

@Query("SELECT u FROM User u WHERE u.id != ?2 AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', ?1, '%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%', ?1, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', ?1, '%')))")
List<User> searchUsers(String query, String currentUserId);
}