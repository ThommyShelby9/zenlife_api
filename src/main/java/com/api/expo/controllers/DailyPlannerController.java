// 4. Modifiez DailyPlannerController pour utiliser DailyPlannerDTO

package com.api.expo.controllers;

import com.api.expo.dto.DailyPlannerDTO;
import com.api.expo.services.DailyPlannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/planner")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class DailyPlannerController {
    
    private final DailyPlannerService dailyPlannerService;
    
    @GetMapping("/today")
    public ResponseEntity<?> getTodayPlanner(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            DailyPlannerDTO planner = dailyPlannerService.getTodayPlanner(userDetails);
            return ResponseEntity.ok(planner);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération du planificateur");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/date/{date}")
    public ResponseEntity<?> getPlannerForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Optional<DailyPlannerDTO> plannerOpt = dailyPlannerService.getPlannerForDate(userDetails, date);
            
            if (plannerOpt.isPresent()) {
                return ResponseEntity.ok(plannerOpt.get());
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "not_found");
                response.put("message", "Aucun planificateur trouvé pour cette date");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération du planificateur");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/history")
    public ResponseEntity<?> getPlannerHistory(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<DailyPlannerDTO> planners = dailyPlannerService.getUserPlanners(userDetails);
            return ResponseEntity.ok(planners);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération de l'historique");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/range")
    public ResponseEntity<?> getPlannersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<DailyPlannerDTO> planners = dailyPlannerService.getUserPlannersInRange(userDetails, start, end);
            return ResponseEntity.ok(planners);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération des planificateurs");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/save")
    public ResponseEntity<?> savePlanner(
            @RequestBody DailyPlannerDTO plannerDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            DailyPlannerDTO savedPlanner = dailyPlannerService.savePlanner(userDetails, plannerDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Planificateur enregistré avec succès");
            response.put("planner", savedPlanner);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de l'enregistrement du planificateur");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


}