package com.api.expo.services;

import com.api.expo.dto.DailyPlannerDTO;
import com.api.expo.dto.TaskDTO;
import com.api.expo.models.DailyPlanner;
import com.api.expo.models.Task;
import com.api.expo.models.User;
import com.api.expo.repository.DailyPlannerRepository;
import com.api.expo.repository.TaskRepository;
import com.api.expo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyPlannerService {
    
    private final DailyPlannerRepository dailyPlannerRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    
    @Transactional
    public DailyPlannerDTO getTodayPlanner(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        LocalDate today = LocalDate.now();
        Optional<DailyPlanner> planner = dailyPlannerRepository.findByUserIdAndDate(user.getId(), today);
            
        if (planner.isPresent()) {
            return new DailyPlannerDTO(planner.get());
        } else {
            // Créer un nouveau planificateur pour aujourd'hui
            DailyPlanner newPlanner = new DailyPlanner();
            newPlanner.setUser(user);
            newPlanner.setDate(today);
            newPlanner.setGoals("");
            newPlanner.setNotes("");
            newPlanner.setGratitudeNotes("");
            newPlanner.setReflection("");
            newPlanner.setMoodRating(0);
                
            return new DailyPlannerDTO(dailyPlannerRepository.save(newPlanner));
        }
    }
    
    @Transactional
    public DailyPlannerDTO savePlanner(UserDetails userDetails, DailyPlannerDTO plannerDTO) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        LocalDate date = LocalDate.parse(plannerDTO.getDate());
        Optional<DailyPlanner> existingPlannerOpt = dailyPlannerRepository.findByUserIdAndDate(user.getId(), date);
            
        DailyPlanner plannerToSave;
            // Mise à jour d'un planificateur existant
            DailyPlanner existingPlanner = existingPlannerOpt.get();
            
            // Mettre à jour les propriétés de base
            existingPlanner.setGoals(plannerDTO.getGoals() != null ? plannerDTO.getGoals() : "");
            existingPlanner.setNotes(plannerDTO.getNotes() != null ? plannerDTO.getNotes() : "");
            existingPlanner.setGratitudeNotes(plannerDTO.getGratitudeNotes() != null ? plannerDTO.getGratitudeNotes() : "");
            
            // IMPORTANT: Mise à jour explicite de reflection et moodRating
            existingPlanner.setReflection(plannerDTO.getReflection() != null ? plannerDTO.getReflection() : "");
            existingPlanner.setMoodRating(plannerDTO.getMoodRating() != null ? plannerDTO.getMoodRating() : 0);
            
            existingPlanner.setUpdatedAt(Instant.now());
            
            // Méthode améliorée pour mettre à jour les tâches
            updatePlannerTasks(existingPlanner, plannerDTO);
            
            plannerToSave = existingPlanner;
        
        // Sauvegarder le planificateur
        DailyPlanner savedPlanner = dailyPlannerRepository.save(plannerToSave);
        return new DailyPlannerDTO(savedPlanner);
    }
    
    // Nouvelle méthode pour gérer la mise à jour des tâches de manière plus sûre
    private void updatePlannerTasks(DailyPlanner planner, DailyPlannerDTO plannerDTO) {
        // Créer une liste de nouvelles tâches
        List<Task> updatedTasks = new ArrayList<>();
        
        if (plannerDTO.getTasks() != null) {
            for (TaskDTO taskDTO : plannerDTO.getTasks()) {
                Task task;
                
                if (taskDTO.getId() != null && !taskDTO.getId().isEmpty()) {
                    // Chercher la tâche existante par ID
                    Optional<Task> existingTaskOpt = planner.getTasks().stream()
                        .filter(t -> t.getId().equals(taskDTO.getId()))
                        .findFirst();
                        
                    if (existingTaskOpt.isPresent()) {
                        // Mettre à jour la tâche existante
                        task = existingTaskOpt.get();
                        task.setTitle(taskDTO.getTitle());
                        task.setNotes(taskDTO.getNotes() != null ? taskDTO.getNotes() : "");
                        task.setPriority(taskDTO.getPriority() != null ? taskDTO.getPriority() : "low");
                        task.setCompleted(taskDTO.isCompleted());
                        task.setDueTime(taskDTO.getDueTime());
                        task.setUpdatedAt(Instant.now());
                    } else {
                        // ID fourni mais tâche non trouvée, créer une nouvelle
                        task = taskDTO.toEntity();
                    }
                } else {
                    // Pas d'ID, créer une nouvelle tâche
                    task = taskDTO.toEntity();
                }
                
                updatedTasks.add(task);
            }
        }
        
        // Utiliser la méthode setter qui gère correctement la collection
        planner.setTasks(updatedTasks);
    }
    
    @Transactional(readOnly = true)
    public List<DailyPlannerDTO> getUserPlanners(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        return dailyPlannerRepository.findByUserIdOrderByDateDesc(user.getId()).stream()
            .map(DailyPlannerDTO::new)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<DailyPlannerDTO> getUserPlannersInRange(UserDetails userDetails, LocalDate start, LocalDate end) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        return dailyPlannerRepository.findByUserIdAndDateBetweenOrderByDateDesc(user.getId(), start, end).stream()
            .map(DailyPlannerDTO::new)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Optional<DailyPlannerDTO> getPlannerForDate(UserDetails userDetails, LocalDate date) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        return dailyPlannerRepository.findByUserIdAndDate(user.getId(), date)
            .map(DailyPlannerDTO::new);
    }
}
