package com.api.expo.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.expo.models.User;
import com.api.expo.models.Validation;
import com.api.expo.repository.UserRepository;
import com.api.expo.repository.ValidationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final ValidationRepository validationRepository;
    private final MailSenderService mailSender;
    private final UserRepository userRepository;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    private String generateValidationCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        String generatedCode = code.toString();
        
        // Vérifie si le code existe déjà
        while (validationRepository.findByCode(generatedCode) != null) {
            code.setLength(0); // Reset le StringBuilder
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
            generatedCode = code.toString();
            System.out.println(generatedCode);
        }
        
        return generatedCode;
    }

    @Transactional
    public User save(User user) {
        User savedUser = userRepository.save(user);

        // Supprimer les anciens codes non utilisés pour cet utilisateur
        validationRepository.deleteByUserAndActivationIsNull(savedUser);

        // Créer une nouvelle validation
        Validation validation = new Validation();
        validation.setUser(savedUser);

        // Configurer les timestamps
        Instant creation = Instant.now();
        validation.setCreation(creation);
        Instant expiration = creation.plus(30, ChronoUnit.MINUTES);
        validation.setExpire(expiration);

        // Générer un code de validation court
        String code = generateValidationCode();
        validation.setCode(code); // Cette ligne utilise maintenant le code court généré

        // Sauvegarder la validation
        this.validationRepository.save(validation);

        // Envoyer l'email avec le code
        this.mailSender.sendValidationEmail(validation);

        return savedUser;
    }

    @Transactional
    public User verifyEmail(String code) {
        Validation validation = validationRepository.findByCode(code);

        if (validation == null) {
            throw new ValidationNotFoundException();
        }

        if (validation.getExpire().isBefore(Instant.now())) {
            throw new ValidationExpiredException("Le code de validation a expiré");
        }

        User user = validation.getUser();
        user.setEnabled(true);
        user.setIsVerified(true);
        user.setEmailVerifyAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        validation.setActivation(Instant.now());
        validationRepository.save(validation);

        return userRepository.save(user);
    }
}