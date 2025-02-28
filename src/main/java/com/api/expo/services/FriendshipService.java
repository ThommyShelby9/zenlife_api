// FriendshipService.java
package com.api.expo.services;

import com.api.expo.models.Friendship;
import com.api.expo.models.User;
import com.api.expo.repository.FriendshipRepository;
import com.api.expo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendshipService {
    
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    public List<User> getUserFriends(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        List<Friendship> friendships = friendshipRepository.findAcceptedFriendshipsByUserId(user.getId());
        
        return friendships.stream()
            .map(friendship -> {
                if (friendship.getRequester().getId().equals(user.getId())) {
                    return friendship.getAddressee();
                } else {
                    return friendship.getRequester();
                }
            })
            .collect(Collectors.toList());
    }
    
    public List<Friendship> getPendingFriendRequests(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        return friendshipRepository.findPendingFriendRequestsForUser(user.getId());
    }
    
    public List<Friendship> getSentFriendRequests(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        return friendshipRepository.findSentFriendRequestsByUser(user.getId());
    }
    
    public Friendship sendFriendRequest(UserDetails userDetails, String addresseeId) {
        User requester = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        User addressee = userRepository.findById(addresseeId)
            .orElseThrow(() -> new RuntimeException("Destinataire non trouvé"));
            
        // Vérifier si une relation d'amitié existe déjà
        Optional<Friendship> existingFriendship = friendshipRepository.findByUserIds(requester.getId(), addressee.getId());
        
        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();
            
            if (friendship.getStatus().equals("ACCEPTED")) {
                throw new RuntimeException("Vous êtes déjà ami avec cet utilisateur");
            } else if (friendship.getStatus().equals("PENDING")) {
                if (friendship.getRequester().getId().equals(requester.getId())) {
                    throw new RuntimeException("Vous avez déjà envoyé une demande d'ami à cet utilisateur");
                } else {
                    // L'autre utilisateur a déjà envoyé une demande, accepter automatiquement
                    return acceptFriendRequest(userDetails, friendship.getId());
                }
            } else if (friendship.getStatus().equals("BLOCKED")) {
                throw new RuntimeException("Vous ne pouvez pas envoyer de demande d'ami à cet utilisateur");
            } else {
                // Réactiver la relation si elle a été refusée
                friendship.setStatus("PENDING");
                friendship.setUpdatedAt(Instant.now());
                Friendship savedFriendship = friendshipRepository.save(friendship);
                
                // Envoyer une notification
                notificationService.createSystemNotification(
                    addressee,
                    "FRIEND_REQUEST",
                    requester.getFullName() + " vous a envoyé une demande d'ami",
                    "/friends/requests"
                );
                
                return savedFriendship;
            }
        } else {
            // Créer une nouvelle relation
            Friendship friendship = new Friendship();
            friendship.setRequester(requester);
            friendship.setAddressee(addressee);
            friendship.setStatus("PENDING");
            
            Friendship savedFriendship = friendshipRepository.save(friendship);
            
            // Envoyer une notification
            notificationService.createSystemNotification(
                addressee,
                "FRIEND_REQUEST",
                requester.getFullName() + " vous a envoyé une demande d'ami",
                "/friends/requests"
            );
            
            return savedFriendship;
        }
    }
    
    public Friendship acceptFriendRequest(UserDetails userDetails, String friendshipId) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new RuntimeException("Demande d'ami non trouvée"));
            
        // Vérifier que l'utilisateur est bien le destinataire de la demande
        if (!friendship.getAddressee().getId().equals(user.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à accepter cette demande d'ami");
        }
        
        // Vérifier que la demande est en attente
        if (!friendship.getStatus().equals("PENDING")) {
            throw new RuntimeException("Cette demande d'ami ne peut pas être acceptée");
        }
        
        friendship.setStatus("ACCEPTED");
        friendship.setAcceptedAt(Instant.now());
        friendship.setUpdatedAt(Instant.now());
        
        Friendship savedFriendship = friendshipRepository.save(friendship);
        
        // Envoyer une notification
        notificationService.createSystemNotification(
            friendship.getRequester(),
            "FRIEND_ACCEPTED",
            user.getFullName() + " a accepté votre demande d'ami",
            "/friends"
        );
        
        return savedFriendship;
    }
    
    public Friendship rejectFriendRequest(UserDetails userDetails, String friendshipId) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new RuntimeException("Demande d'ami non trouvée"));
            
        // Vérifier que l'utilisateur est bien le destinataire de la demande
        if (!friendship.getAddressee().getId().equals(user.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à rejeter cette demande d'ami");
        }
        
        // Vérifier que la demande est en attente
        if (!friendship.getStatus().equals("PENDING")) {
            throw new RuntimeException("Cette demande d'ami ne peut pas être rejetée");
        }
        
        friendship.setStatus("REJECTED");
        friendship.setUpdatedAt(Instant.now());
        
        return friendshipRepository.save(friendship);
    }
    
    public void removeFriend(UserDetails userDetails, String friendId) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        User friend = userRepository.findById(friendId)
            .orElseThrow(() -> new RuntimeException("Ami non trouvé"));
            
        Optional<Friendship> existingFriendship = friendshipRepository.findByUserIds(user.getId(), friend.getId());
        
        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();
            
            // Vérifier que les utilisateurs sont amis
            if (!friendship.getStatus().equals("ACCEPTED")) {
                throw new RuntimeException("Vous n'êtes pas ami avec cet utilisateur");
            }
            
            friendshipRepository.delete(friendship);
        } else {
            throw new RuntimeException("Relation d'amitié non trouvée");
        }
    }
    
    public Friendship blockUser(UserDetails userDetails, String userToBlockId) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        User userToBlock = userRepository.findById(userToBlockId)
            .orElseThrow(() -> new RuntimeException("Utilisateur à bloquer non trouvé"));
            
        Optional<Friendship> existingFriendship = friendshipRepository.findByUserIds(user.getId(), userToBlock.getId());
        
        Friendship friendship;
        if (existingFriendship.isPresent()) {
            friendship = existingFriendship.get();
            friendship.setStatus("BLOCKED");
            friendship.setUpdatedAt(Instant.now());
            
            // S'assurer que l'utilisateur qui bloque est le demandeur
            if (friendship.getRequester().getId().equals(userToBlock.getId())) {
                // Inverser les rôles pour que l'utilisateur qui bloque soit le demandeur
                friendship.setRequester(user);
                friendship.setAddressee(userToBlock);
            }
        } else {
            friendship = new Friendship();
            friendship.setRequester(user);
            friendship.setAddressee(userToBlock);
            friendship.setStatus("BLOCKED");
        }
        
        return friendshipRepository.save(friendship);
    }
    
    public void unblockUser(UserDetails userDetails, String blockedUserId) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        User blockedUser = userRepository.findById(blockedUserId)
            .orElseThrow(() -> new RuntimeException("Utilisateur bloqué non trouvé"));
            
        Optional<Friendship> existingFriendship = friendshipRepository.findByUserIds(user.getId(), blockedUser.getId());
        
        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();
            
            // Vérifier que l'utilisateur est celui qui a bloqué
            if (friendship.getStatus().equals("BLOCKED") && friendship.getRequester().getId().equals(user.getId())) {
                friendshipRepository.delete(friendship);
            } else {
                throw new RuntimeException("Vous n'avez pas bloqué cet utilisateur");
            }
        } else {
            throw new RuntimeException("Relation non trouvée");
        }
    }
}