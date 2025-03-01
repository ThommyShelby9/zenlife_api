package com.api.expo.repository;

import com.api.expo.models.UserOnlineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserOnlineStatusRepository extends JpaRepository<UserOnlineStatus, String> {
    
    List<UserOnlineStatus> findByOnlineTrue();
    
    // Correction de la requête pour utiliser userId (propriété correcte) au lieu de userIds
    @Query("SELECT u FROM UserOnlineStatus u WHERE u.userId IN :userIds AND u.online = true")
    List<UserOnlineStatus> findOnlineUsersByUserIds(@Param("userIds") List<String> userIds);
}