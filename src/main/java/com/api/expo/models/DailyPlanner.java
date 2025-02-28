package com.api.expo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "DAILY_PLANNERS")
public class DailyPlanner {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(columnDefinition = "TEXT")
    private String goals;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "gratitude_notes", columnDefinition = "TEXT")
    private String gratitudeNotes;
    
    @Column(name = "reflection", columnDefinition = "TEXT")
    private String reflection;
    
    @Column(name = "mood_rating")
    private Integer moodRating;
    
    // Modification de la relation avec Task
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "planner_id", nullable = false) // Au lieu de daily_planner_id
    private List<Task> tasks = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    public DailyPlanner() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.tasks = new ArrayList<>();
    }
    
    // Méthode helper pour gérer correctement la relation bidirectionnelle
    public void setTasks(List<Task> tasks) {
        // Vider la liste actuelle
        this.tasks.clear();
        
        // Ajouter les nouvelles tâches
        if (tasks != null) {
            this.tasks.addAll(tasks);
        }
    }
    
    // Méthode helper pour ajouter une tâche
    public void addTask(Task task) {
        this.tasks.add(task);
    }
    
    // Méthode helper pour supprimer une tâche
    public void removeTask(Task task) {
        this.tasks.remove(task);
    }
}