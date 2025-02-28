// WaterIntake.java
package com.api.expo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "WATER_INTAKES")
public class WaterIntake {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private Integer quantityML;
    
    @Column(name = "intake_time", nullable = false)
    private Instant intakeTime;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    public WaterIntake() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.intakeTime = Instant.now();
    }
}