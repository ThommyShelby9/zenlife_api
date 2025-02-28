package com.api.expo.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.expo.models.User;
import com.api.expo.models.Validation;
import com.api.expo.repository.UserRepository;
import com.api.expo.repository.ValidationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    // Constants
    private static final int VERIFICATION_EXPIRATION_MINUTES = 30;
    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
    private static final String INVALID_PASSWORD_MESSAGE = "Le mot de passe doit contenir au moins 8 caractères, une lettre majuscule, "
            +
            "une lettre minuscule, un chiffre et un caractère spécial.";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;

    // Dependencies
    private final UserRepository userRepository;
    private final ValidationRepository validationRepository;
    private final MailSenderService mailSender;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();
    // Configuration
    @Value("${app.frontend.url}")
    private String frontendUrl;

    // Code Generation Method
    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                builder.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
            code = builder.toString();
        } while (validationRepository.findByCode(code) != null);

        return code;
    }

    // Password Reset Methods
    public void initiatePasswordReset(String email) {
        User user = findUserByEmail(email);
        String resetToken = generateUniqueCode();
        updateUserForPasswordReset(user, resetToken);
        mailSender.sendResetEmail(user, resetToken);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = findUserByResetToken(token);
        validateNewPassword(newPassword);
        updateUserPassword(user, newPassword);
    }

    // Email Verification Methods
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = findUserByEmail(email);
        validateUserNotVerified(user);
        invalidateExistingValidations(user);
        Validation newValidation = createNewValidation(user);
        mailSender.sendValidationEmail(newValidation);
    }

    // Private Helper Methods - User Operations
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Aucun utilisateur trouvé avec cet email"));
    }

    private User findUserByResetToken(String token) {
        return userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Token invalide"));
    }

    private void updateUserForPasswordReset(User user, String resetToken) {
        user.setVerificationToken(resetToken);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    private void updateUserPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setVerificationToken(null);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    private void validateUserNotVerified(User user) {
        if (user.getIsVerified()) {
            throw new RuntimeException("Cet utilisateur est déjà vérifié");
        }
    }

    // Private Helper Methods - Validation Operations
    private void invalidateExistingValidations(User user) {
        validationRepository.findByUser(user).forEach(validation -> {
            validation.setExpire(Instant.now());
            validationRepository.save(validation);
        });
    }

    private Validation createNewValidation(User user) {
        Validation validation = new Validation();
        validation.setUser(user);
        validation.setCreation(Instant.now());
        validation.setExpire(Instant.now().plus(VERIFICATION_EXPIRATION_MINUTES, ChronoUnit.MINUTES));

        String code = generateUniqueCode();
        validation.setCode(code);
        validation.setVerificationLink(generateVerificationLink(code));

        return validationRepository.save(validation);
    }

    // Private Helper Methods - Utilities
    private void validateNewPassword(String password) {
        if (!isPasswordValid(password)) {
            throw new IllegalArgumentException(INVALID_PASSWORD_MESSAGE);
        }
    }

    private boolean isPasswordValid(String password) {
        return password.matches(PASSWORD_PATTERN);
    }

    private String generateVerificationLink(String code) {
        try {
            return String.format("%s/verify?code=%s",
                    frontendUrl,
                    URLEncoder.encode(code, StandardCharsets.UTF_8.toString()));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du lien de vérification", e);
        }
    }

}