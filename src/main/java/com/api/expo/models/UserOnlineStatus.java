package com.api.expo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "USER_ONLINE_STATUS")
@Getter
@Setter
public class UserOnlineStatus {

    @Id
    private String userId;
    
    private Instant lastActivity;
    
    private boolean online;
    
    public UserOnlineStatus() {}
    
    public UserOnlineStatus(String userId) {
        this.userId = userId;
        this.lastActivity = Instant.now();
        this.online = true;
    }
    
    public void updateActivity() {
        this.lastActivity = Instant.now();
        this.online = true;
    }
}