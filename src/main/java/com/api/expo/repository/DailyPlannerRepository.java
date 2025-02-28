// DailyPlannerRepository.java
package com.api.expo.repository;

import com.api.expo.models.DailyPlanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyPlannerRepository extends JpaRepository<DailyPlanner, String> {
    Optional<DailyPlanner> findByUserIdAndDate(String userId, LocalDate date);
    List<DailyPlanner> findByUserIdOrderByDateDesc(String userId);
    List<DailyPlanner> findByUserIdAndDateBetweenOrderByDateDesc(String userId, LocalDate startDate, LocalDate endDate);
}