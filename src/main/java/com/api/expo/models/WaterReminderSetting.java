// WaterReminderSetting.java
package com.api.expo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "WATER_REMINDER_SETTINGS")
public class WaterReminderSetting {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private Integer dailyGoalML;
    
    @Column(nullable = false)
    private Integer reminderIntervalMinutes;
    
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @Column(nullable = false)
    private Instant startTime; // Début de la période de rappels quotidiens
    
    @Column(nullable = false)
    private Instant endTime; // Fin de la période de rappels quotidiens
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    public WaterReminderSetting() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
