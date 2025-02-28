// UpdatePasswordRequest.java
package com.api.expo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePasswordRequest {
    private String currentPassword;
    private String newPassword;
}