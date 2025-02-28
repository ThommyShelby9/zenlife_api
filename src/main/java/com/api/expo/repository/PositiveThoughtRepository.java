// PositiveThoughtRepository.java
package com.api.expo.repository;

import com.api.expo.models.PositiveThought;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PositiveThoughtRepository extends JpaRepository<PositiveThought, String> {
    List<PositiveThought> findByCategory(String category);
    
    @Query(value = "SELECT * FROM positive_thoughts WHERE category = ?1 ORDER BY RAND() LIMIT 1", nativeQuery = true)
    PositiveThought findRandomByCategory(String category);
    
    @Query(value = "SELECT * FROM positive_thoughts ORDER BY RAND() LIMIT 1", nativeQuery = true)
    PositiveThought findRandom();
}