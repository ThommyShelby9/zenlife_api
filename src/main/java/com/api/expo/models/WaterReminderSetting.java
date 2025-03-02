package com.api.expo.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    
    @JsonProperty("reminderEnabled")
    public Boolean getReminderEnabled() {
        return this.enabled;
    }
    
    @JsonProperty("reminderEnabled")
    public void setReminderEnabled(Boolean reminderEnabled) {
        this.enabled = reminderEnabled;
    }
    
    @JsonProperty("reminderInterval")
    public Integer getReminderInterval() {
        return this.reminderIntervalMinutes;
    }
    
    @JsonProperty("reminderInterval")
    public void setReminderInterval(Integer reminderInterval) {
        this.reminderIntervalMinutes = reminderInterval;
    }
    
    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")   
     private Instant startTime; // Début de la période de rappels quotidiens
    
    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
        private Instant endTime; // Fin de la période de rappels quotidiens
    
    @ElementCollection
    @CollectionTable(name = "CUSTOM_GLASS_SIZES", joinColumns = @JoinColumn(name = "water_reminder_setting_id"))
    @Column(name = "size_ml")
    private List<Integer> customGlassSizes = new ArrayList<>();
    
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