// PositiveThoughtRepository.java
package com.api.expo.repository;

import com.api.expo.models.PositiveThought;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Random;

@Repository
public interface PositiveThoughtRepository extends JpaRepository<PositiveThought, String> {
    List<PositiveThought> findByCategory(String category);

    default PositiveThought findRandomByCategory(String category) {
        List<PositiveThought> thoughts = findByCategory(category);
        if (thoughts.isEmpty()) return null;
        return thoughts.get(new Random().nextInt(thoughts.size()));
    }

    default PositiveThought findRandom() {
        List<PositiveThought> all = findAll();
        if (all.isEmpty()) return null;
        return all.get(new Random().nextInt(all.size()));
    }
}