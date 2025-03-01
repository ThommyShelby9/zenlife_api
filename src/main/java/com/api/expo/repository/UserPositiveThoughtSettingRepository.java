// UserPositiveThoughtSettingRepository.java
package com.api.expo.repository;

import com.api.expo.models.UserPositiveThoughtSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPositiveThoughtSettingRepository extends JpaRepository<UserPositiveThoughtSetting, String> {
    Optional<UserPositiveThoughtSetting> findByUserId(String userId);

    List<UserPositiveThoughtSetting> findByEnabledTrue();
}