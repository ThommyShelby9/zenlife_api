// WaterIntakeRepository.java
package com.api.expo.repository;

import com.api.expo.models.WaterIntake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface WaterIntakeRepository extends JpaRepository<WaterIntake, String> {
    List<WaterIntake> findByUserIdOrderByIntakeTimeDesc(String userId);
    
    @Query("SELECT wi FROM WaterIntake wi WHERE wi.user.id = ?1 AND wi.intakeTime BETWEEN ?2 AND ?3 ORDER BY wi.intakeTime DESC")
    List<WaterIntake> findByUserIdAndDateRange(String userId, Instant start, Instant end);
    
    @Query("SELECT SUM(wi.quantityML) FROM WaterIntake wi WHERE wi.user.id = ?1 AND wi.intakeTime BETWEEN ?2 AND ?3")
    Integer getTotalIntakeForUserInRange(String userId, Instant start, Instant end);
}