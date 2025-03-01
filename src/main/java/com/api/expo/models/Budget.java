package com.api.expo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "BUDGETS")
public class Budget {
    
    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private ExpenseCategory category; // Null signifie budget global
    
    @Column(nullable = false)
    private String yearMonthStr; // Stockage sous forme de String (format YYYY-MM)
    
    @Transient // Cette propriété ne sera pas stockée directement
    private YearMonth yearMonth;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(name = "alert_threshold_percentage", nullable = false)
    private Integer alertThresholdPercentage = 80; // Alerte à 80% par défaut
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    // Ajouter la propriété notes
    @Column(nullable = true)
    private String notes;
        
        public Budget() {
            this.id = UUID.randomUUID().toString();
            this.createdAt = Instant.now();
            this.updatedAt = Instant.now();
        }
        
        // Getters et setters pour la conversion
        
        // Convertir le YearMonth en String avant de sauvegarder
        @PrePersist
        @PreUpdate
        private void beforeSave() {
            if (yearMonth != null) {
                this.yearMonthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            }
        }
        
        // Convertir le String en YearMonth après le chargement
        @PostLoad
        private void afterLoad() {
            if (yearMonthStr != null && !yearMonthStr.isEmpty()) {
                this.yearMonth = YearMonth.parse(yearMonthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
            }
        }
        
        public YearMonth getYearMonth() {
            return yearMonth;
        }
        
        public void setYearMonth(YearMonth yearMonth) {
            this.yearMonth = yearMonth;
            this.yearMonthStr = yearMonth != null ? yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")) : null;
        }
    
        public void setNotes(String notes) {
            this.notes = notes;
    }
}