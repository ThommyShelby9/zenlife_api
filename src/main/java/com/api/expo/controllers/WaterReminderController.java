package com.api.expo.controllers;

import com.api.expo.models.WaterIntake;
import com.api.expo.models.WaterReminderSetting;
import com.api.expo.services.WaterReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/water")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class WaterReminderController {
    
    private final WaterReminderService waterReminderService;
    
    @GetMapping("/settings")
    public ResponseEntity<?> getUserSettings(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            WaterReminderSetting settings = waterReminderService.getUserSettings(userDetails);
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération des paramètres");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PutMapping("/settings")
    public ResponseEntity<?> updateUserSettings(
            @RequestBody WaterReminderSetting settings,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            WaterReminderSetting updatedSettings = waterReminderService.updateUserSettings(userDetails, settings);
            return ResponseEntity.ok(updatedSettings);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la mise à jour des paramètres");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/log")
    public ResponseEntity<?> logWaterIntake(
            @RequestBody Map<String, Integer> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Integer quantityML = payload.get("quantityML");
            if (quantityML == null || quantityML <= 0) {
                throw new IllegalArgumentException("La quantité doit être positive");
            }
            
            WaterIntake intake = waterReminderService.logWaterIntake(userDetails, quantityML);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Consommation d'eau enregistrée");
            response.put("intake", intake);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de l'enregistrement de la consommation d'eau");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/history")
    public ResponseEntity<?> getWaterIntakeHistory(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<WaterIntake> history = waterReminderService.getUserIntakeHistory(userDetails);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération de l'historique");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/progress")
    public ResponseEntity<?> getDailyProgress(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Map<String, Object> progress = waterReminderService.getDailyProgress(userDetails);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération de la progression");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Exporte les données d'hydratation de l'utilisateur au format CSV
     */
    @GetMapping("/export/csv")
    public ResponseEntity<Resource> exportCsv(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            byte[] csvData = waterReminderService.exportDataToCsv(userDetails);
            
            ByteArrayResource resource = new ByteArrayResource(csvData);
            
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String filename = "hydratation_" + today + ".csv";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .contentLength(csvData.length)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Exporte les données d'hydratation de l'utilisateur au format Excel
     */
    @GetMapping("/export/excel")
    public ResponseEntity<Resource> exportExcel(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            byte[] excelData = waterReminderService.exportDataToExcel(userDetails);
            
            ByteArrayResource resource = new ByteArrayResource(excelData);
            
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String filename = "hydratation_" + today + ".xlsx";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(excelData.length)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Exporte les données d'hydratation de l'utilisateur au format PDF
     */
    @GetMapping("/export/pdf")
    public ResponseEntity<Resource> exportPdf(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            byte[] pdfData = waterReminderService.exportDataToPdf(userDetails);
            
            ByteArrayResource resource = new ByteArrayResource(pdfData);
            
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String filename = "hydratation_" + today + ".pdf";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfData.length)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Réinitialise l'historique d'hydratation de l'utilisateur
     */
    @PostMapping("/reset-history")
    public ResponseEntity<?> resetHistory(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            waterReminderService.resetWaterIntakeHistory(userDetails);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Historique réinitialisé avec succès");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la réinitialisation de l'historique");
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}