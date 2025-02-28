// FriendshipController.java
package com.api.expo.controllers;

import com.api.expo.models.Friendship;
import com.api.expo.models.User;
import com.api.expo.services.FriendshipService;
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
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class FriendshipController {
    
    private final FriendshipService friendshipService;
    
    @GetMapping
    public ResponseEntity<?> getUserFriends(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<User> friends = friendshipService.getUserFriends(userDetails);
            return ResponseEntity.ok(friends);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération des amis");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/requests/pending")
    public ResponseEntity<?> getPendingFriendRequests(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<Friendship> requests = friendshipService.getPendingFriendRequests(userDetails);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération des demandes d'ami");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/requests/sent")
    public ResponseEntity<?> getSentFriendRequests(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<Friendship> requests = friendshipService.getSentFriendRequests(userDetails);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération des demandes d'ami envoyées");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/request/{userId}")
    public ResponseEntity<?> sendFriendRequest(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Friendship friendship = friendshipService.sendFriendRequest(userDetails, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Demande d'ami envoyée avec succès");
            response.put("friendship", friendship);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de l'envoi de la demande d'ami");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/accept/{friendshipId}")
    public ResponseEntity<?> acceptFriendRequest(
            @PathVariable String friendshipId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Friendship friendship = friendshipService.acceptFriendRequest(userDetails, friendshipId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Demande d'ami acceptée avec succès");
            response.put("friendship", friendship);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de l'acceptation de la demande d'ami");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/reject/{friendshipId}")
    public ResponseEntity<?> rejectFriendRequest(
            @PathVariable String friendshipId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Friendship friendship = friendshipService.rejectFriendRequest(userDetails, friendshipId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Demande d'ami rejetée avec succès");
            response.put("friendship", friendship);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors du rejet de la demande d'ami");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @DeleteMapping("/remove/{friendId}")
    public ResponseEntity<?> removeFriend(
            @PathVariable String friendId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            friendshipService.removeFriend(userDetails, friendId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Ami supprimé avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la suppression de l'ami");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/block/{userId}")
    public ResponseEntity<?> blockUser(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Friendship friendship = friendshipService.blockUser(userDetails, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Utilisateur bloqué avec succès");
            response.put("friendship", friendship);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors du blocage de l'utilisateur");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/unblock/{userId}")
    public ResponseEntity<?> unblockUser(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            friendshipService.unblockUser(userDetails, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Utilisateur débloqué avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors du déblocage de l'utilisateur");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
