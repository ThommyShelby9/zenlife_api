// WaterReminderService.java
package com.api.expo.services;

import com.api.expo.models.User;
import com.api.expo.models.WaterIntake;
import com.api.expo.models.WaterReminderSetting;
import com.api.expo.repository.UserRepository;
import com.api.expo.repository.WaterIntakeRepository;
import com.api.expo.repository.WaterReminderSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WaterReminderService {
    
    private final WaterReminderSettingRepository waterReminderSettingRepository;
    private final WaterIntakeRepository waterIntakeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SimpMessageSendingOperations messagingTemplate;
    
    public WaterReminderSetting getUserSettings(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        Optional<WaterReminderSetting> settings = waterReminderSettingRepository.findByUserId(user.getId());
        if (settings.isPresent()) {
            return settings.get();
        } else {
            // Créer des paramètres par défaut si inexistants
            WaterReminderSetting defaultSettings = new WaterReminderSetting();
            defaultSettings.setUser(user);
            defaultSettings.setDailyGoalML(user.getDailyWaterGoalML() != null ? user.getDailyWaterGoalML() : 2000);
            defaultSettings.setReminderIntervalMinutes(60); // Rappel toutes les heures par défaut
            defaultSettings.setEnabled(true);
            
            // Définir les heures de début et de fin par défaut (8h00 à 22h00)
            Instant now = Instant.now();
            Instant startTime = now.truncatedTo(ChronoUnit.DAYS).plus(8, ChronoUnit.HOURS);
            Instant endTime = now.truncatedTo(ChronoUnit.DAYS).plus(22, ChronoUnit.HOURS);
            
            defaultSettings.setStartTime(startTime);
            defaultSettings.setEndTime(endTime);
            
            return waterReminderSettingRepository.save(defaultSettings);
        }
    }
    
    public WaterReminderSetting updateUserSettings(UserDetails userDetails, WaterReminderSetting updatedSettings) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        Optional<WaterReminderSetting> existingSettings = waterReminderSettingRepository.findByUserId(user.getId());
        
        WaterReminderSetting settingsToSave;
        if (existingSettings.isPresent()) {
            settingsToSave = existingSettings.get();
            settingsToSave.setDailyGoalML(updatedSettings.getDailyGoalML());
            settingsToSave.setReminderIntervalMinutes(updatedSettings.getReminderIntervalMinutes());
            settingsToSave.setEnabled(updatedSettings.getEnabled());
            settingsToSave.setStartTime(updatedSettings.getStartTime());
            settingsToSave.setEndTime(updatedSettings.getEndTime());
            settingsToSave.setUpdatedAt(Instant.now());
        } else {
            settingsToSave = updatedSettings;
            settingsToSave.setUser(user);
            settingsToSave.setCreatedAt(Instant.now());
            settingsToSave.setUpdatedAt(Instant.now());
        }
        
        // Mettre également à jour l'objectif quotidien d'eau dans le profil utilisateur
        user.setDailyWaterGoalML(updatedSettings.getDailyGoalML());
        userRepository.save(user);
        
        return waterReminderSettingRepository.save(settingsToSave);
    }
    
    public WaterIntake logWaterIntake(UserDetails userDetails, Integer quantityML) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        WaterIntake intake = new WaterIntake();
        intake.setUser(user);
        intake.setQuantityML(quantityML);
        intake.setIntakeTime(Instant.now());
        
        return waterIntakeRepository.save(intake);
    }
    
    public List<WaterIntake> getUserIntakeHistory(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        return waterIntakeRepository.findByUserIdOrderByIntakeTimeDesc(user.getId());
    }
    
    public Map<String, Object> getDailyProgress(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        // Calculer le début et la fin de la journée
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        // Obtenir l'objectif quotidien
        Integer dailyGoal = user.getDailyWaterGoalML();
        if (dailyGoal == null) {
            Optional<WaterReminderSetting> settings = waterReminderSettingRepository.findByUserId(user.getId());
            dailyGoal = settings.map(WaterReminderSetting::getDailyGoalML).orElse(2000);
        }
        
        // Obtenir la consommation totale d'aujourd'hui
        Integer totalIntake = waterIntakeRepository.getTotalIntakeForUserInRange(user.getId(), startOfDay, endOfDay);
        if (totalIntake == null) {
            totalIntake = 0;
        }
        
        // Calculer le pourcentage d'avancement
        int percentage = (int) Math.min(100, (totalIntake * 100.0) / dailyGoal);
        
        Map<String, Object> result = new HashMap<>();
        result.put("dailyGoalML", dailyGoal);
        result.put("currentIntakeML", totalIntake);
        result.put("remainingML", Math.max(0, dailyGoal - totalIntake));
        result.put("percentage", percentage);
        
        return result;
    }
    
    public void sendWaterReminder(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        Optional<WaterReminderSetting> settingsOpt = waterReminderSettingRepository.findByUserId(userId);
        if (settingsOpt.isPresent() && settingsOpt.get().getEnabled()) {
            // Créer une notification pour rappeler de boire de l'eau
            try {
                notificationService.createSystemNotification(
                    user,
                    "WATER_REMINDER",
                    "Rappel pour boire de l'eau ! 💦",
                    "/water-tracker"
                );
            } catch (Exception e) {
                // Gérer les exceptions de notification
                System.out.println("Erreur lors de l'envoi du rappel d'eau: " + e.getMessage());
            }
        }
    }
}