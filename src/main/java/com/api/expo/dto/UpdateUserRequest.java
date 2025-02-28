// UpdateUserRequest.java
package com.api.expo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private String fullName;
    private String firstname;
    private String lastname;
    private String username;
    private String bio;
    private Integer dailyWaterGoalML;
    private String notificationPreferences;
    private String themePreference;
    private MultipartFile profilePicture;
}