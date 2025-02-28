// UserRepository.java (mise à jour)
package com.api.expo.repository;

import com.api.expo.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByVerificationToken(String token);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', ?1, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', ?1, '%'))")
    List<User> findByUsernameOrFullNameContainingIgnoreCase(String searchTerm);
}
