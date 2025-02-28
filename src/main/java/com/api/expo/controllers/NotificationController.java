// NotificationController.java (mise à jour avec les nouvelles fonctionnalités)
package com.api.expo.controllers;

import com.api.expo.models.Notification;
import com.api.expo.services.NotificationService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class NotificationController {
    
    private final NotificationService notificationService;
    
    // Créer une nouvelle notification
    @PostMapping("/notifications/create")
    public ResponseEntity<?> createNotification(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Notification notification = notificationService.createNotification(
                userDetails,
                payload.get("type"),
                payload.get("content"),
                payload.get("link")
            );
            
            response.put("status", "success");
            response.put("message", "Notification créée avec succès");
            response.put("notification", notification);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Erreur lors de la création de la notification: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors de la création de la notification");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Récupérer toutes les notifications d'un utilisateur
    @GetMapping("/notifications")
    public ResponseEntity<?> getUserNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Notification> notifications = notificationService.getUserNotifications(userDetails);
            return ResponseEntity.ok(notifications);
            
        } catch (Exception e) {
            System.out.println("Erreur lors de la récupération des notifications: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération des notifications");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

// Récupérer les notifications non lues
@GetMapping("/notifications/unread")
public ResponseEntity<?> getUnreadNotifications(
        @AuthenticationPrincipal UserDetails userDetails) {
    
    Map<String, Object> response = new HashMap<>();
    
    try {
        List<Notification> notifications = notificationService.getUnreadNotifications(userDetails);
        return ResponseEntity.ok(notifications);
        
    } catch (Exception e) {
        System.out.println("Erreur lors de la récupération des notifications non lues: " + e.getMessage());
        e.printStackTrace();
        
        response.put("status", "error");
        response.put("message", "Erreur lors de la récupération des notifications non lues");
        response.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

// Compter les notifications non lues
@GetMapping("/notifications/count")
public ResponseEntity<?> countUnreadNotifications(
        @AuthenticationPrincipal UserDetails userDetails) {
    
    Map<String, Object> response = new HashMap<>();
    
    try {
        long count = notificationService.countUnreadNotifications(userDetails);
        response.put("count", count);
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        System.out.println("Erreur lors du comptage des notifications non lues: " + e.getMessage());
        e.printStackTrace();
        
        response.put("status", "error");
        response.put("message", "Erreur lors du comptage des notifications non lues");
        response.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

// Marquer une notification comme lue
@PutMapping("/notifications/{notificationId}/read")
public ResponseEntity<?> markAsRead(
        @PathVariable String notificationId,
        @AuthenticationPrincipal UserDetails userDetails) {
    
    Map<String, Object> response = new HashMap<>();
    
    try {
        notificationService.markAsRead(notificationId, userDetails);
        
        response.put("status", "success");
        response.put("message", "Notification marquée comme lue");
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        System.out.println("Erreur lors du marquage de la notification: " + e.getMessage());
        e.printStackTrace();
        
        response.put("status", "error");
        response.put("message", "Erreur lors du marquage de la notification");
        response.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

// Marquer toutes les notifications comme lues
@PutMapping("/notifications/read-all")
public ResponseEntity<?> markAllAsRead(
        @AuthenticationPrincipal UserDetails userDetails) {
    
    Map<String, Object> response = new HashMap<>();
    
    try {
        notificationService.markAllAsRead(userDetails);
        
        response.put("status", "success");
        response.put("message", "Toutes les notifications ont été marquées comme lues");
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        System.out.println("Erreur lors du marquage de toutes les notifications: " + e.getMessage());
        e.printStackTrace();
        
        response.put("status", "error");
        response.put("message", "Erreur lors du marquage de toutes les notifications");
        response.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

// Supprimer une notification
@DeleteMapping("/notifications/{notificationId}")
public ResponseEntity<?> deleteNotification(
        @PathVariable String notificationId,
        @AuthenticationPrincipal UserDetails userDetails) {
    
    Map<String, Object> response = new HashMap<>();
    
    try {
        notificationService.deleteNotification(notificationId, userDetails);
        
        response.put("status", "success");
        response.put("message", "Notification supprimée avec succès");
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        System.out.println("Erreur lors de la suppression de la notification: " + e.getMessage());
        e.printStackTrace();
        
        response.put("status", "error");
        response.put("message", "Erreur lors de la suppression de la notification");
        response.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
}