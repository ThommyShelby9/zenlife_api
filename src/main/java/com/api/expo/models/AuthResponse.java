package com.api.expo.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private User user;
    private String token;
}

