// PositiveThoughtController.java
package com.api.expo.controllers;

import com.api.expo.models.PositiveThought;
import com.api.expo.models.UserPositiveThoughtSetting;
import com.api.expo.services.PositiveThoughtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/positive-thoughts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class PositiveThoughtController {
    
    private final PositiveThoughtService positiveThoughtService;
    
    @GetMapping("/random")
    public ResponseEntity<?> getRandomThought(
            @RequestParam(required = false) String category) {
        try {
            PositiveThought thought = positiveThoughtService.getRandomPositiveThought(category);
            return ResponseEntity.ok(thought);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération de la pensée positive");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getAllThoughts() {
        try {
            List<PositiveThought> thoughts = positiveThoughtService.getAllPositiveThoughts();
            return ResponseEntity.ok(thoughts);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération des pensées positives");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getThoughtsByCategory(@PathVariable String category) {
        try {
            List<PositiveThought> thoughts = positiveThoughtService.getPositiveThoughtsByCategory(category);
            return ResponseEntity.ok(thoughts);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération des pensées positives");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createThought(
            @RequestBody PositiveThought thought) {
        try {
            PositiveThought savedThought = positiveThoughtService.createPositiveThought(thought);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Pensée positive créée avec succès");
            response.put("thought", savedThought);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la création de la pensée positive");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/settings")
    public ResponseEntity<?> getUserSettings(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            UserPositiveThoughtSetting settings = positiveThoughtService.getUserSettings(userDetails);
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
            @RequestBody UserPositiveThoughtSetting settings,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            UserPositiveThoughtSetting updatedSettings = positiveThoughtService.updateUserSettings(userDetails, settings);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Paramètres mis à jour avec succès");
            response.put("settings", updatedSettings);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la mise à jour des paramètres");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}