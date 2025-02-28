package com.api.expo.dto;

import com.api.expo.models.Task;
import lombok.Data;
import lombok.NoArgsConstructor; // Ajout de cette annotation

import java.time.Instant;

@Data
@NoArgsConstructor // Cette annotation garantit qu'un constructeur par défaut existe
public class TaskDTO {
    private String id;
    private String title;
    private String notes;
    private String priority;
    private boolean completed;
    private String dueTime;
    private String createdAt;
    private String updatedAt;
    
    // Le constructeur par défaut est généré par l'annotation @NoArgsConstructor
    
    // Constructeur à partir de l'entité Task
    public TaskDTO(Task task) {
        this.id = task.getId();
        this.title = task.getTitle();
        this.notes = task.getNotes();
        this.priority = task.getPriority();
        this.completed = task.isCompleted();
        this.dueTime = task.getDueTime();
        this.createdAt = task.getCreatedAt() != null ? task.getCreatedAt().toString() : null;
        this.updatedAt = task.getUpdatedAt() != null ? task.getUpdatedAt().toString() : null;
    }
    
    // Méthode pour convertir DTO en entité
    public Task toEntity() {
        Task task = new Task();
        
        // Conserver l'ID si présent, sinon un nouveau sera généré
        if (this.id != null && !this.id.isEmpty()) {
            task.setId(this.id);
        }
        
        // Propriétés requises
        task.setTitle(this.title);
        
        // Propriétés optionnelles avec valeurs par défaut
        task.setNotes(this.notes != null ? this.notes : "");
        task.setPriority(this.priority != null ? this.priority : "low");
        task.setCompleted(this.completed);
        task.setDueTime(this.dueTime);
        
        // Timestamps
        task.setCreatedAt(this.createdAt != null ? Instant.parse(this.createdAt) : Instant.now());
        task.setUpdatedAt(this.updatedAt != null ? Instant.parse(this.updatedAt) : Instant.now());
        
        return task;
    }
}