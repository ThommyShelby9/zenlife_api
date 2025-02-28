package com.api.expo.dto;

import com.api.expo.models.DailyPlanner;
import com.api.expo.models.Task;
import lombok.Data;
import lombok.NoArgsConstructor; // Ajout de cette annotation

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor // Cette annotation garantit qu'un constructeur par défaut existe
public class DailyPlannerDTO {
    private String id;
    private String userId;
    private String date;
    private List<TaskDTO> tasks = new ArrayList<>();
    private String reflection;
    private Integer moodRating;
    private Integer completedCount;
    private String createdAt;
    private String updatedAt;
    
    // Pour compatibilité avec le backend existant
    private String goals;
    private String notes;
    private String gratitudeNotes;
    
    // Le constructeur par défaut est généré par l'annotation @NoArgsConstructor
    
    // Constructeur à partir de l'entité
    public DailyPlannerDTO(DailyPlanner planner) {
        this.id = planner.getId();
        this.userId = planner.getUser() != null ? planner.getUser().getId() : null;
        this.date = planner.getDate().toString();
        
        // Convertir les tâches en DTO
        this.tasks = planner.getTasks().stream()
                .map(TaskDTO::new)
                .collect(Collectors.toList());
                
        // Calcul du nombre de tâches complétées
        this.completedCount = (int) planner.getTasks().stream()
                .filter(Task::isCompleted)
                .count();
                
        this.reflection = planner.getReflection();
        this.moodRating = planner.getMoodRating();
        this.goals = planner.getGoals();
        this.notes = planner.getNotes();
        this.gratitudeNotes = planner.getGratitudeNotes();
        this.createdAt = planner.getCreatedAt().toString();
        this.updatedAt = planner.getUpdatedAt().toString();
    }
    
    // Méthode pour convertir DTO en entité
    public DailyPlanner toEntity() {
        DailyPlanner planner = new DailyPlanner();
        if (this.id != null) {
            planner.setId(this.id);
        }
        
        if (this.date != null) {
            planner.setDate(LocalDate.parse(this.date));
        }
        
        // Convertir les TaskDTO en Task
        if (this.tasks != null) {
            planner.setTasks(this.tasks.stream()
                    .map(TaskDTO::toEntity)
                    .collect(Collectors.toList()));
        }
        
        planner.setReflection(this.reflection);
        planner.setMoodRating(this.moodRating);
        planner.setGoals(this.goals != null ? this.goals : "");
        planner.setNotes(this.notes != null ? this.notes : "");
        planner.setGratitudeNotes(this.gratitudeNotes != null ? this.gratitudeNotes : "");
        
        planner.setCreatedAt(this.createdAt != null ? Instant.parse(this.createdAt) : Instant.now());
        planner.setUpdatedAt(Instant.now());
        
        return planner;
    }
}