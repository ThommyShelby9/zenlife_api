// Mise à jour du UserController.java pour inclure les fonctionnalités de gestion de compte
package com.api.expo.controllers;

import com.api.expo.dto.UpdateUserRequest;
import com.api.expo.models.User;
import com.api.expo.repository.UserRepository;
import com.api.expo.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {
    
    private final UserRepository userRepository;
    private final UserService userService;
    
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam String query,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (query.length() < 3) {
                response.put("status", "error");
                response.put("message", "La requête de recherche doit contenir au moins 3 caractères");
                return ResponseEntity.badRequest().body(response);
            }
            
            List<User> users = userRepository.findByUsernameOrFullNameContainingIgnoreCase(query);
            
            // Filtrer l'utilisateur actuel de la liste des résultats
            users = users.stream()
                .filter(user -> !user.getEmail().equals(userDetails.getUsername()))
                .toList();
            
            return ResponseEntity.ok(users);
            
        } catch (Exception e) {
            System.out.println("Erreur lors de la recherche d'utilisateurs: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors de la recherche d'utilisateurs");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserProfile(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userService.getUser(userId);
            
            // Ne pas exposer des informations sensibles
            user.setPassword(null);
            
            return ResponseEntity.ok(user);
            
        } catch (Exception e) {
            System.out.println("Erreur lors de la récupération du profil: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération du profil");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userService.getCurrentUser();
            
            // Ne pas exposer des informations sensibles
            user.setPassword(null);
            
            return ResponseEntity.ok(user);
            
        } catch (Exception e) {
            System.out.println("Erreur lors de la récupération du profil: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération du profil");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody UpdateUserRequest updatedUser,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userService.updateUser(userDetails.getUsername(), updatedUser);
            
            // Ne pas exposer des informations sensibles
            user.setPassword(null);
            
            response.put("status", "success");
            response.put("message", "Profil mis à jour avec succès");
            response.put("user", user);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Erreur lors de la mise à jour du profil: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors de la mise à jour du profil");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/profile/picture")
    public ResponseEntity<?> updateProfilePicture(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            UpdateUserRequest updateRequest = new UpdateUserRequest();
            updateRequest.setProfilePicture(file);
            
            User user = userService.updateUser(userDetails.getUsername(), updateRequest);
            
            response.put("status", "success");
            response.put("message", "Photo de profil mise à jour avec succès");
            response.put("url", user.getProfilePictureUrl());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Erreur lors de la mise à jour de la photo de profil: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors de la mise à jour de la photo de profil");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/deactivate")
    public ResponseEntity<?> deactivateAccount(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            userService.deactivateAccount(userDetails.getUsername());
            
            response.put("status", "success");
            response.put("message", "Votre compte a été désactivé");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Erreur lors de la désactivation du compte: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors de la désactivation du compte");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/reactivate")
    public ResponseEntity<?> reactivateAccount(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = request.get("email");
            userService.sendReactivationEmail(email);
            
            response.put("status", "success");
            response.put("message", "Un email de réactivation a été envoyé");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Erreur lors de l'envoi de l'email de réactivation: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors de l'envoi de l'email de réactivation");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/notify-preferences")
    public ResponseEntity<?> updateNotificationPreferences(
            @RequestBody Map<String, String> preferences,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userService.updateNotificationPreferences(
                userDetails.getUsername(), 
                preferences.get("notificationPreferences")
            );
            
            response.put("status", "success");
            response.put("message", "Préférences de notification mises à jour");
            response.put("preferences", user.getNotificationPreferences());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Erreur lors de la mise à jour des préférences: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors de la mise à jour des préférences");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/theme-preference")
    public ResponseEntity<?> updateThemePreference(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userService.updateThemePreference(
                userDetails.getUsername(), 
                request.get("themePreference")
            );
            
            response.put("status", "success");
            response.put("message", "Préférence de thème mise à jour");
            response.put("themePreference", user.getThemePreference());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Erreur lors de la mise à jour de la préférence de thème: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors de la mise à jour de la préférence de thème");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}