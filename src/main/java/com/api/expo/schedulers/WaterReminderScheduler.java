package com.api.expo.schedulers;

import com.api.expo.models.User;
import com.api.expo.models.WaterReminderSetting;
import com.api.expo.repository.UserRepository;
import com.api.expo.repository.WaterReminderSettingRepository;
import com.api.expo.services.WaterReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WaterReminderScheduler {

    private final WaterReminderSettingRepository waterReminderSettingRepository;
    private final UserRepository userRepository;
    private final WaterReminderService waterReminderService;

    /**
     * Vérifie toutes les heures si des rappels d'eau doivent être envoyés aux utilisateurs
     * En fonction de leurs paramètres individuels
     */
    @Scheduled(cron = "0 0 * * * *") // Exécute à chaque heure pile
    public void checkAndSendWaterReminders() {
        log.info("Vérification des rappels d'hydratation...");
        Instant now = Instant.now();
        LocalTime currentTime = LocalTime.now(ZoneId.systemDefault());
        
        // Récupérer tous les paramètres de rappel actifs
        List<WaterReminderSetting> activeSettings = waterReminderSettingRepository.findAll();
        
        // Pour chaque utilisateur avec des rappels activés
        for (WaterReminderSetting setting : activeSettings) {
            if (setting.getEnabled()) {
                try {
                    // Vérifier si l'heure actuelle est dans la plage horaire définie par l'utilisateur
                    LocalTime startTime = LocalTime.ofInstant(setting.getStartTime(), ZoneId.systemDefault());
                    LocalTime endTime = LocalTime.ofInstant(setting.getEndTime(), ZoneId.systemDefault());
                    
                    // Récupérer l'intervalle de rappel
                    int interval = setting.getReminderIntervalMinutes();
                    
                    // Si l'heure actuelle est dans la plage définie par l'utilisateur
                    if (isTimeInRange(currentTime, startTime, endTime)) {
                        // Calculer le temps écoulé depuis le début de la journée en minutes
                        int minutesSinceMidnight = currentTime.getHour() * 60 + currentTime.getMinute();
                        
                        // Si nous sommes à un multiple de l'intervalle (±5 minutes pour permettre une certaine tolérance)
                        if (minutesSinceMidnight % interval <= 5 || 
                            (interval - (minutesSinceMidnight % interval)) <= 5) {
                            
                            log.info("Envoi d'un rappel d'hydratation à l'utilisateur: {}", setting.getUser().getUsername());
                            // Envoyer le rappel si l'utilisateur n'a pas atteint son objectif quotidien
                            if (!waterReminderService.hasReachedDailyGoal(setting.getUser().getId())) {
                                waterReminderService.sendWaterReminder(setting.getUser().getId());
                            }
                        }
                    }
                } catch (Exception e) {
                    // Gérer les erreurs pour éviter d'arrêter le traitement pour les autres utilisateurs
                    log.error("Erreur lors du traitement des rappels d'eau pour l'utilisateur {} : {}", 
                              setting.getUser().getId(), e.getMessage(), e);
                }
            }
        }
        log.info("Vérification des rappels d'hydratation terminée");
    }
    
    /**
     * Vérifie si une heure est dans une plage définie
     */
    private boolean isTimeInRange(LocalTime time, LocalTime start, LocalTime end) {
        // Gère également le cas où la période traverse minuit
        if (start.isBefore(end)) {
            return !time.isBefore(start) && !time.isAfter(end);
        } else {
            return !time.isBefore(start) || !time.isAfter(end);
        }
    }
}