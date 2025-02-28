// WaterReminderSettingRepository.java
package com.api.expo.repository;

import com.api.expo.models.WaterReminderSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WaterReminderSettingRepository extends JpaRepository<WaterReminderSetting, String> {
    Optional<WaterReminderSetting> findByUserId(String userId);
}