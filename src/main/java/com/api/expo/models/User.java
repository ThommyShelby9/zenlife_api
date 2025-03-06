// User.java (mise à jour)
package com.api.expo.models;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "USERS")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    @JsonProperty(value = "fullName")
    private String fullName;

    @JsonProperty(value = "lastname")
    private String lastName; 

    @JsonProperty(value = "firstname")
    private String firstName; 

    @JsonProperty(value = "username")
    private String username; 

    @JsonProperty(value = "password")
    private String password;

    @JsonProperty(value = "email")
    private String email;
    
    // Nouveaux champs pour ZenLife
    private String bio;
    
    @Column(name = "daily_water_goal_ml")
    private Integer dailyWaterGoalML = 2000; // Par défaut 2L
    
    @Column(name = "notification_preferences", columnDefinition = "TEXT")
    private String notificationPreferences = "ALL"; // ALL, WATER_ONLY, FINANCE_ONLY, NONE
    
    @Column(name = "theme_preference")
    private String themePreference = "LIGHT"; // LIGHT, DARK, SYSTEM

    @Column(nullable = false)
    private Boolean accountNonExpired = true;

    @Column(nullable = false)
    private Boolean accountNonLocked = true;

    @Column(nullable = false)
    private Boolean credentialsNonExpired = true;

    @Column(nullable = false)
    private Boolean enabled = false;

    private Instant emailVerifyAt;

    private String profilePicture;
    
    @JsonProperty("profilePictureUrl")
    public String getProfilePictureUrl() {
        if (profilePicture != null) {
            // Vérifier si c'est déjà une URL complète (Cloudinary)
            if (profilePicture.startsWith("http")) {
                return profilePicture;
            }
            // Sinon c'est un fichier local
            return "/api/files/profile-pictures/" + profilePicture;
        }
        return null;
    }
    
    private Instant createdAt;
    private Instant updatedAt;

    private String verificationToken;
    
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;
   
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email; // Utiliser l'email comme username pour la connexion
    }
    
    public String getUserDisplayName() {
        return this.username; // Pour l'affichage
    }
    
    public String getLastname() {
        return this.lastName;
    }

    public String getFirstname() {
        return this.firstName;
    }

    public Instant getEmailVerifyAt() {
        return this.emailVerifyAt;
    }

    public Instant setEmailVerifyAt(Instant emailVerifyAt) {
         return this.emailVerifyAt = emailVerifyAt;
    }

    public String getEmail() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
    
    public Boolean getIsVerified() {
        return this.isVerified;
    }

    public void setIsVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // Constructeur sans arguments
    public User() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.id = UUID.randomUUID().toString();
    }

    // Constructeur avec un UUID
    public User(UUID id) {
        this.id = UUID.randomUUID().toString();
    }

    public void setProfilePictureUrl(String imageUrl) {
        // Stocke l'URL telle quelle, qu'elle soit locale ou Cloudinary
        this.profilePicture = imageUrl;
    }
}