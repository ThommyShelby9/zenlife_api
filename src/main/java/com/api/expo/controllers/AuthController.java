// AuthController.java
package com.api.expo.controllers;

import com.api.expo.config.JwtService;
import com.api.expo.dto.AuthRequest;
import com.api.expo.dto.RegisterRequest;
import com.api.expo.dto.UpdatePasswordRequest;
import com.api.expo.models.AuthResponse;
import com.api.expo.models.User;
import com.api.expo.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private final UserService userService;
    private final JwtService jwtService;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User registeredUser = userService.register(registerRequest);
            
            response.put("status", "success");
            response.put("message", "Inscription réussie ! Veuillez vérifier votre email pour activer votre compte.");
            response.put("user", registeredUser);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            AuthResponse authResponse = userService.login(authRequest.getEmail(), authRequest.getPassword());
            
            return ResponseEntity.ok(authResponse);
        } catch (BadCredentialsException e) {
            response.put("status", "error");
            response.put("message", "Email ou mot de passe incorrect");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Note: La déconnexion côté serveur peut être gérée en invalidant le token JWT
            // mais en pratique, c'est souvent le client qui se charge de supprimer le token
            
            response.put("status", "success");
            response.put("message", "Déconnexion réussie");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/verify-email/{token}")
    public ResponseEntity<?> verifyEmail(@PathVariable String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User verifiedUser = userService.verifyEmail(token);
            
            response.put("status", "success");
            response.put("message", "Votre email a été vérifié avec succès ! Vous pouvez maintenant vous connecter.");
            response.put("user", verifiedUser);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = request.get("email");
            userService.resendVerificationEmail(email);
            
            response.put("status", "success");
            response.put("message", "Un nouvel email de vérification a été envoyé.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = request.get("email");
            userService.sendPasswordResetEmail(null, email);
            
            response.put("status", "success");
            response.put("message", "Un email de réinitialisation de mot de passe a été envoyé.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/reset-password/{token}")
    public ResponseEntity<?> resetPassword(
            @PathVariable String token,
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String newPassword = request.get("password");
            userService.resetPassword(token, newPassword);
            
            response.put("status", "success");
            response.put("message", "Votre mot de passe a été réinitialisé avec succès.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody UpdatePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            userService.changePassword(
                userDetails.getUsername(),
                request.getCurrentPassword(),
                request.getNewPassword()
            );
            
            response.put("status", "success");
            response.put("message", "Votre mot de passe a été modifié avec succès.");
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            response.put("status", "error");
            response.put("message", "Le mot de passe actuel est incorrect");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String token = request.get("token");
            
            // Extraire l'email du token
            String email = jwtService.extractUsername(token);
            
            // Charger les détails de l'utilisateur
            UserDetails userDetails = jwtService.loadUserByEmail(email);
            
            // Valider le token avec les détails de l'utilisateur
            boolean isValid = jwtService.validateToken(token, userDetails);
            
            if (isValid) {
                response.put("status", "success");
                response.put("valid", true);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("valid", false);
                response.put("message", "Token invalide ou expiré");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("valid", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}