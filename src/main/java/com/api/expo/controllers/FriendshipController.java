package com.api.expo.controllers;

import com.api.expo.models.Friendship;
import com.api.expo.models.User;
import com.api.expo.repository.FriendshipRepository;
import com.api.expo.repository.UserRepository;
import com.api.expo.services.FriendshipService;
import com.api.expo.services.NotificationService;
import com.api.expo.services.UserOnlineStatusService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la gestion des amitiés et relations sociales
 */
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class FriendshipController {
    
    private final FriendshipService friendshipService;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserOnlineStatusService userOnlineStatusService;
    private final NotificationService notificationService;
    
    //==========================================================================
    // Endpoints de récupération de données
    //==========================================================================
    
    /**
     * Récupère la liste des amis de l'utilisateur connecté
     */
    @GetMapping
    public ResponseEntity<?> getUserFriends(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Récupérer l'utilisateur actuel
            User currentUser = getUserFromUserDetails(userDetails);
            
            // Récupérer les amis via le service
            List<User> friends = friendshipService.getUserFriends(userDetails);
            
            // Récupérer les IDs des amis
            List<String> friendIds = friends.stream()
                .map(User::getId)
                .collect(Collectors.toList());
            
            // Récupérer le statut en ligne de tous les amis
            Map<String, Boolean> onlineStatus = userOnlineStatusService.getOnlineStatusForUsers(friendIds);
            
            // Construire la réponse
            List<Map<String, Object>> response = friends.stream()
                .map(friend -> mapUserWithOnlineStatus(friend, onlineStatus))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des amis", e);
        }
    }
    
    /**
     * Récupère les demandes d'amis reçues en attente
     */
    @GetMapping("/requests/pending")
    public ResponseEntity<?> getPendingFriendRequests(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = getUserFromUserDetails(userDetails);
            List<Friendship> friendships = friendshipService.getPendingFriendRequests(userDetails);
            
            List<Map<String, Object>> requests = friendships.stream()
                .map(this::mapFriendshipToRequestResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des demandes d'ami", e);
        }
    }
    
    /**
     * Récupère les demandes d'amis envoyées en attente
     */
    @GetMapping("/requests/sent")
    public ResponseEntity<?> getSentFriendRequests(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = getUserFromUserDetails(userDetails);
            List<Friendship> friendships = friendshipService.getSentFriendRequests(userDetails);
            
            List<Map<String, Object>> requests = friendships.stream()
                .map(this::mapFriendshipToRequestResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des demandes d'ami envoyées", e);
        }
    }
    
    /**
     * Récupère la liste des utilisateurs bloqués
     */
    @GetMapping("/blocked")
    public ResponseEntity<?> getBlockedUsers(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getUserFromUserDetails(userDetails);
            List<Friendship> blockships = friendshipRepository.findByRequesterIdAndStatus(user.getId(), "BLOCKED");
            
            List<Map<String, Object>> blockedUsers = blockships.stream()
                .map(friendship -> {
                    User blockedUser = friendship.getAddressee();
                    Map<String, Object> formattedUser = mapUserToBasicResponse(blockedUser);
                    formattedUser.put("blockedAt", friendship.getUpdatedAt().toString());
                    return formattedUser;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(blockedUsers);
        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des utilisateurs bloqués", e);
        }
    }
    
    /**
     * Recherche des utilisateurs par nom, email ou username
     */
    @GetMapping("/users/search")
public ResponseEntity<?> searchUsers(@RequestParam("q") String query, @AuthenticationPrincipal UserDetails userDetails) {
    try {
        User currentUser = getUserFromUserDetails(userDetails);
        
        // Utiliser la méthode findBySearchQuery avec les bons paramètres
        List<User> users = userRepository.findBySearchQuery(userDetails.getUsername(), query);
        
        List<Map<String, Object>> formattedUsers = users.stream()
            .map(user -> {
                Map<String, Object> formattedUser = mapUserToBasicResponse(user);
                
                // Déterminer le statut d'amitié
                String friendStatus = "none";
                Optional<Friendship> friendship = friendshipRepository.findByUserIds(currentUser.getId(), user.getId());
                if (friendship.isPresent()) {
                    friendStatus = friendship.get().getStatus().toLowerCase();
                    if (friendStatus.equals("accepted")) {
                        friendStatus = "friend";
                    }
                }
                
                formattedUser.put("friendStatus", friendStatus);
                return formattedUser;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(formattedUsers);
    } catch (Exception e) {
        return createErrorResponse("Erreur lors de la recherche d'utilisateurs", e);
    }
}
    
    //==========================================================================
    // Endpoints de gestion des demandes d'amis
    //==========================================================================
    
    /**
     * Envoie une demande d'ami à un utilisateur
     */
    @PostMapping("/request/{userId}")
    public ResponseEntity<?> sendFriendRequest(@PathVariable String userId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Friendship friendship = friendshipService.sendFriendRequest(userDetails, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Demande d'ami envoyée avec succès");
            response.put("friendship", mapFriendshipToBasicInfo(friendship));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Erreur lors de l'envoi de la demande d'ami", e);
        }
    }
    
    /**
     * Accepte une demande d'ami
     */
    @PostMapping("/accept/{friendshipId}")
    public ResponseEntity<?> acceptFriendRequest(@PathVariable String friendshipId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Friendship friendship = friendshipService.acceptFriendRequest(userDetails, friendshipId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Demande d'ami acceptée avec succès");
            response.put("friendship", mapFriendshipToBasicInfo(friendship));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Erreur lors de l'acceptation de la demande d'ami", e);
        }
    }
    
    /**
     * Rejette une demande d'ami
     */
    @PostMapping("/reject/{friendshipId}")
    public ResponseEntity<?> rejectFriendRequest(@PathVariable String friendshipId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Friendship friendship = friendshipService.rejectFriendRequest(userDetails, friendshipId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Demande d'ami rejetée avec succès");
            response.put("friendship", mapFriendshipToBasicInfo(friendship));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Erreur lors du rejet de la demande d'ami", e);
        }
    }
    
    /**
     * Annule une demande d'ami envoyée
     */
    @DeleteMapping("/request/{friendshipId}")
    public ResponseEntity<?> cancelFriendRequest(@PathVariable String friendshipId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getUserFromUserDetails(userDetails);
            Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Demande d'ami non trouvée"));
            
            // Vérifier que l'utilisateur est le demandeur
            if (!friendship.getRequester().getId().equals(user.getId())) {
                throw new RuntimeException("Vous n'êtes pas autorisé à annuler cette demande d'ami");
            }
            
            // Vérifier que la demande est en attente
            if (!friendship.getStatus().equals("PENDING")) {
                throw new RuntimeException("Cette demande d'ami ne peut pas être annulée");
            }
            
            friendshipRepository.delete(friendship);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Demande d'ami annulée avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Erreur lors de l'annulation de la demande d'ami", e);
        }
    }
    
    //==========================================================================
    // Endpoints de gestion des amitiés
    //==========================================================================
    
    /**
     * Supprime une amitié existante
     */
    @DeleteMapping("/remove/{friendId}")
    public ResponseEntity<?> removeFriend(@PathVariable String friendId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            friendshipService.removeFriend(userDetails, friendId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Ami supprimé avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la suppression de l'ami", e);
        }
    }
    
    /**
     * Bloque un utilisateur
     */
    @PostMapping("/block/{userId}")
    public ResponseEntity<?> blockUser(@PathVariable String userId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Friendship friendship = friendshipService.blockUser(userDetails, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Utilisateur bloqué avec succès");
            response.put("friendship", mapFriendshipToBasicInfo(friendship));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Erreur lors du blocage de l'utilisateur", e);
        }
    }
    
    /**
     * Débloque un utilisateur
     */
    @PostMapping("/unblock/{userId}")
    public ResponseEntity<?> unblockUser(@PathVariable String userId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            friendshipService.unblockUser(userDetails, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Utilisateur débloqué avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Erreur lors du déblocage de l'utilisateur", e);
        }
    }
    
    //==========================================================================
    // Méthodes utilitaires
    //==========================================================================
    
    /**
     * Récupère l'objet User à partir des détails d'authentification
     */
    private User getUserFromUserDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
    
    /**
     * Crée une réponse d'erreur standardisée
     */
    private ResponseEntity<?> createErrorResponse(String message, Exception e) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        response.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * Mappe un utilisateur en réponse de base
     */
    private Map<String, Object> mapUserToBasicResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put("fullName", user.getFullName());
        userResponse.put("username", user.getUsername());
        userResponse.put("email", user.getEmail());
        userResponse.put("profilePictureUrl", user.getProfilePictureUrl());
        return userResponse;
    }
    
    /**
     * Mappe un utilisateur avec son statut en ligne
     */
    private Map<String, Object> mapUserWithOnlineStatus(User user, Map<String, Boolean> onlineStatus) {
        Map<String, Object> userResponse = mapUserToBasicResponse(user);
        userResponse.put("online", onlineStatus.getOrDefault(user.getId(), false));
        return userResponse;
    }
    
    /**
     * Mappe une relation d'amitié vers la réponse d'une demande
     */
    private Map<String, Object> mapFriendshipToRequestResponse(Friendship friendship) {
        Map<String, Object> request = new HashMap<>();
        request.put("id", friendship.getId());
        
        // Le sender est toujours le requester
        Map<String, Object> sender = mapUserToBasicResponse(friendship.getRequester());
        
        // Le recipient est toujours l'addressee
        Map<String, Object> recipient = mapUserToBasicResponse(friendship.getAddressee());
        
        request.put("sender", sender);
        request.put("recipient", recipient);
        request.put("status", friendship.getStatus().toLowerCase());
        request.put("timestamp", friendship.getCreatedAt().toString());
        
        return request;
    }
    
    /**
     * Mappe une relation d'amitié vers un format de base
     */
    private Map<String, Object> mapFriendshipToBasicInfo(Friendship friendship) {
        Map<String, Object> friendshipInfo = new HashMap<>();
        friendshipInfo.put("id", friendship.getId());
        friendshipInfo.put("status", friendship.getStatus());
        friendshipInfo.put("createdAt", friendship.getCreatedAt().toString());
        friendshipInfo.put("updatedAt", friendship.getUpdatedAt().toString());
        if (friendship.getAcceptedAt() != null) {
            friendshipInfo.put("acceptedAt", friendship.getAcceptedAt().toString());
        }
        
        // Ajouter les informations sur les utilisateurs
        Map<String, Object> requester = mapUserToBasicResponse(friendship.getRequester());
        Map<String, Object> addressee = mapUserToBasicResponse(friendship.getAddressee());
        
        friendshipInfo.put("requester", requester);
        friendshipInfo.put("addressee", addressee);
        
        return friendshipInfo;
    }
}