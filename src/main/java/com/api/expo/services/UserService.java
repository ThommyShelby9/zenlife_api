package com.api.expo.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.api.expo.config.JwtService;
import com.api.expo.dto.RegisterRequest;
import com.api.expo.dto.UpdateUserRequest;
import com.api.expo.exceptions.UnauthorizedAccessException;
import com.api.expo.exceptions.AuthExecptions.AuthenticationException;
import com.api.expo.models.AuthResponse;
import com.api.expo.models.User;
import com.api.expo.repository.UserRepository;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final FileService fileService;
    private final JavaMailSender mailSender;
    
    @Value("${app.email.from}")
    private String emailFrom;
    
    @Value("${app.frontend.url}")
    private String frontendUrl;

   @Autowired
public UserService(
        UserRepository userRepository,
        BCryptPasswordEncoder passwordEncoder,
        JwtService jwtService,
        FileService fileService,  // Remarquez le nom correct ici
        JavaMailSender mailSender,
        EntityManager entityManager) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.fileService = fileService;
    this.mailSender = mailSender;
}

@Transactional
public User register(RegisterRequest registerRequest) throws MessagingException {
    // Vérifier si l'email existe déjà
    Optional<User> existingUserByEmail = userRepository.findByEmail(registerRequest.getEmail());
    if (existingUserByEmail.isPresent()) {
        throw new RuntimeException("Cet email est déjà utilisé");
    }
    
    // Vérifier si le nom d'utilisateur existe déjà (s'il est fourni)
    if (registerRequest.getUsername() != null && !registerRequest.getUsername().isEmpty()) {
        Optional<User> existingUserByUsername = userRepository.findByUsername(registerRequest.getUsername());
        if (existingUserByUsername.isPresent()) {
            throw new RuntimeException("Ce nom d'utilisateur est déjà pris");
        }
    }

    // Vérifier si l'email est valide
    if (registerRequest.getEmail() == null || !registerRequest.getEmail().contains("@")) {
        throw new IllegalArgumentException("Votre e-mail n'est pas valide!");
    }

    // Vérifier le format du mot de passe
    if (!isPasswordValid(registerRequest.getPassword())) {
        throw new IllegalArgumentException(
                "Le mot de passe doit contenir au moins 8 caractères, une lettre majuscule, une lettre minuscule, un chiffre et un caractère spécial.");
    }

    // Créer le nouvel utilisateur
    User user = new User();
    user.setEmail(registerRequest.getEmail());
    user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
    
    // Définir le nom d'utilisateur (généré automatiquement si non fourni)
    String username = registerRequest.getUsername();
    if (username == null || username.isEmpty()) {
        username = generateUsername(registerRequest.getFirstName(), registerRequest.getLastName());
    }
    user.setUsername(username);
    
    // Définir les noms
    user.setFirstName(registerRequest.getFirstName());  // Modifié: getFirstname -> getFirstName
    user.setLastName(registerRequest.getLastName());    // Modifié: getLastname -> getLastName
    
    // Utiliser le fullName fourni ou le construire à partir de firstName et lastName
    if (registerRequest.getFullName() != null && !registerRequest.getFullName().isEmpty()) {
        user.setFullName(registerRequest.getFullName());
    } else {
        user.setFullName(registerRequest.getFirstName() + " " + registerRequest.getLastName());
    }

    // Définir les valeurs par défaut
    user.setEnabled(false);
    user.setIsVerified(false);
    user.setAccountNonExpired(true);
    user.setAccountNonLocked(true);
    user.setCredentialsNonExpired(true);
    user.setCreatedAt(Instant.now());
    user.setUpdatedAt(Instant.now());
    
    // Définir les valeurs par défaut de ZenLife
    user.setDailyWaterGoalML(2000); // 2L par défaut
    user.setNotificationPreferences("ALL");
    user.setThemePreference("LIGHT");
    
    // Générer un token de vérification
    String verificationToken = UUID.randomUUID().toString();
    user.setVerificationToken(verificationToken);

    // Sauvegarder l'utilisateur
    User savedUser = userRepository.save(user);
    
    // Envoyer l'email de vérification
    sendVerificationEmail(savedUser);

    return savedUser;
}

    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        if (!user.getIsVerified()) {
            throw new AuthenticationException("Veuillez vérifier votre email avant de vous connecter");
        }
        
        if (!user.isEnabled()) {
            throw new AuthenticationException("Votre compte a été désactivé");
        }

        String token = jwtService.generateToken(user);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setUser(user);
        authResponse.setToken(token);

        return authResponse;
    }

    public User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private String generateUsername(String firstName, String lastName) {
        String baseUsername = (firstName + lastName).toLowerCase()
                .replaceAll("[^a-z0-9]", ""); // Supprimer tous les caractères spéciaux
        
        if (baseUsername.length() < 3) {
            baseUsername = baseUsername + "user"; // Assurer une longueur minimale
        }
        
        String username = baseUsername;
        int counter = 1;
        
        // Vérifier si le nom d'utilisateur existe déjà, sinon ajouter un nombre
        while (userRepository.findByUsername(username).isPresent()) {
            username = baseUsername + counter;
            counter++;
        }
    
        return username;
    }

    private boolean isPasswordValid(String password) {
        // Au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        return password.matches(passwordPattern);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'email: " + email));
    }

    @Transactional
    public User updateUser(String currentUserEmail, UpdateUserRequest updateRequest) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
    
        // Vérifier si le nom d'utilisateur est déjà pris (seulement si fourni)
        if (updateRequest.getUsername() != null && !updateRequest.getUsername().isEmpty() && 
            !updateRequest.getUsername().equals(user.getUsername())) {
            userRepository.findByUsername(updateRequest.getUsername())
                    .ifPresent(u -> {
                        throw new RuntimeException("Ce nom d'utilisateur est déjà pris");
                    });
            user.setUsername(updateRequest.getUsername());
        }
        
        // Mise à jour des champs modifiables
        if (updateRequest.getFullName() != null && !updateRequest.getFullName().isEmpty()) {
            user.setFullName(updateRequest.getFullName());
        } else if (updateRequest.getFirstname() != null && updateRequest.getLastname() != null) {
            // Si fullName n'est pas fourni mais firstname et lastname le sont, mettre à jour le fullName
            user.setFullName(updateRequest.getFirstname() + " " + updateRequest.getLastname());
        }
        
        // Mise à jour des autres champs si fournis
        if (updateRequest.getFirstname() != null) {
            user.setFirstName(updateRequest.getFirstname());
        }
        
        if (updateRequest.getLastname() != null) {
            user.setLastName(updateRequest.getLastname());
        }
        
        // Mise à jour des champs spécifiques à ZenLife
        if (updateRequest.getBio() != null) {
            user.setBio(updateRequest.getBio());
        }
        
        if (updateRequest.getDailyWaterGoalML() != null) {
            user.setDailyWaterGoalML(updateRequest.getDailyWaterGoalML());
        }
        
        if (updateRequest.getNotificationPreferences() != null) {
            user.setNotificationPreferences(updateRequest.getNotificationPreferences());
        }
        
        if (updateRequest.getThemePreference() != null) {
            user.setThemePreference(updateRequest.getThemePreference());
        }
    
        // Gérer l'upload de l'image de profil
        MultipartFile profilePicture = updateRequest.getProfilePicture();
        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                String imageUrl = fileService.storeProfilePicture(user, profilePicture);
                user.setProfilePicture(imageUrl);
            } catch (Exception e) {
                throw new RuntimeException("Impossible de sauvegarder l'image de profil", e);
            }
        }
        
        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }
    
    public User verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Token de vérification invalide"));
        
        user.setIsVerified(true);
        user.setEnabled(true);
        user.setEmailVerifyAt(Instant.now());
        user.setVerificationToken(null); // Effacer le token après vérification
        user.setUpdatedAt(Instant.now());
        
        return userRepository.save(user);
    }
    
    public void resendVerificationEmail(String email) throws MessagingException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Aucun compte n'existe avec cet email"));
        
        if (user.getIsVerified()) {
            throw new RuntimeException("Ce compte est déjà vérifié");
        }
        
        // Générer un nouveau token
        String newToken = UUID.randomUUID().toString();
        user.setVerificationToken(newToken);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        
        // Envoyer le nouvel email
        sendVerificationEmail(user);
    }
    
    public void sendVerificationEmail(User user) throws MessagingException {
        String subject = "ZenLife - Vérification de votre adresse email";
        
        // Assurez-vous que l'URL contient /auth/ pour correspondre aux routes frontend
        String verificationLink = frontendUrl + "/auth/verify-email/" + user.getVerificationToken();
        
        String content = generateVerificationEmailContent(user.getFullName(), verificationLink);
        sendHtmlEmail(user.getEmail(), subject, content);
    }
    
public void sendPasswordResetEmail(User user, String resetCode) throws MessagingException {
    String subject = "ZenLife - Code de réinitialisation de votre mot de passe";
    String content = generatePasswordResetEmailContent(user.getFullName(), resetCode);
    sendHtmlEmail(user.getEmail(), subject, content);
}
    
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(emailFrom);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true indique que le contenu est au format HTML
        
        mailSender.send(message);
    }

    private String generateVerificationEmailContent(String fullName, String verificationLink) {
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "    <meta charset=\"utf-8\">\n" +
               "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
               "    <title>Vérification de votre adresse email</title>\n" +
               "</head>\n" +
               "<body style=\"margin: 0; padding: 0; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #333; background-color: #f8f9fa;\">\n" +
               "    <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"center\" width=\"100%\" style=\"max-width: 600px; margin: 0 auto; padding: 40px 20px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);\">\n" +
               "        <tr>\n" +
               "            <td style=\"text-align: center; padding-bottom: 30px;\">\n" +
               "                <img src=\"https://zenlife-gs2w.onrender.com/img/logo.png\" alt=\"ZenLife\" width=\"150\" style=\"display: inline-block; max-width: 100%; height: auto;\">\n" +
               "            </td>\n" +
               "        </tr>\n" +
               "        <tr>\n" +
               "            <td style=\"padding: 0 30px;\">\n" +
               "                <h1 style=\"color: #4f46e5; font-size: 24px; font-weight: 600; margin-bottom: 20px;\">Bienvenue sur ZenLife !</h1>\n" +
               "                <p style=\"color: #4b5563; font-size: 16px; line-height: 1.6; margin-bottom: 25px;\">Bonjour " + fullName + ",</p>\n" +
               "                <p style=\"color: #4b5563; font-size: 16px; line-height: 1.6; margin-bottom: 25px;\">Nous sommes ravis de vous accueillir sur ZenLife. Pour commencer à profiter de tous nos services, veuillez confirmer votre adresse email en cliquant sur le bouton ci-dessous :</p>\n" +
               "                <div style=\"text-align: center; margin: 40px 0;\">\n" +
               "                    <a href=\"" + verificationLink + "\" style=\"display: inline-block; background-color: #4f46e5; color: #ffffff; font-size: 16px; font-weight: 500; text-decoration: none; padding: 12px 30px; border-radius: 6px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);\">Vérifier mon email</a>\n" +
               "                </div>\n" +
               "                <p style=\"color: #4b5563; font-size: 16px; line-height: 1.6; margin-bottom: 15px;\">Ce lien est valable pendant 24 heures. Si vous n'avez pas créé de compte sur ZenLife, vous pouvez ignorer cet email.</p>\n" +
               "                <p style=\"color: #4b5563; font-size: 16px; line-height: 1.6; margin-bottom: 15px;\">Si le bouton ne fonctionne pas, copiez et collez le lien suivant dans votre navigateur :</p>\n" +
               "                <p style=\"color: #6b7280; font-size: 14px; line-height: 1.4; margin-bottom: 30px; word-break: break-all; background-color: #f3f4f6; padding: 12px; border-radius: 4px;\">" + verificationLink + "</p>\n" +
               "            </td>\n" +
               "        </tr>\n" +
               "        <tr>\n" +
               "            <td style=\"padding: 30px; border-top: 1px solid #e5e7eb; text-align: center;\">\n" +
               "                <p style=\"color: #6b7280; font-size: 14px; margin-bottom: 10px;\">Cordialement,</p>\n" +
               "                <p style=\"color: #4f46e5; font-size: 16px; font-weight: 500; margin-bottom: 20px;\">L'équipe ZenLife</p>\n" +
               "                <div style=\"margin-top: 20px;\">\n" +
               "                    <a href=\"#\" style=\"display: inline-block; margin: 0 8px;\"><img src=\"" + frontendUrl + "/assets/img/social/facebook.png\" alt=\"Facebook\" width=\"24\"></a>\n" +
               "                    <a href=\"#\" style=\"display: inline-block; margin: 0 8px;\"><img src=\"" + frontendUrl + "/assets/img/social/twitter.png\" alt=\"Twitter\" width=\"24\"></a>\n" +
               "                    <a href=\"#\" style=\"display: inline-block; margin: 0 8px;\"><img src=\"" + frontendUrl + "/assets/img/social/instagram.png\" alt=\"Instagram\" width=\"24\"></a>\n" +
               "                </div>\n" +
               "            </td>\n" +
               "        </tr>\n" +
               "    </table>\n" +
               "</body>\n" +
               "</html>";
    }
    private String generatePasswordResetEmailContent(String fullName, String resetCode) {
    return "<!DOCTYPE html>\n" +
           "<html>\n" +
           "<head>\n" +
           "    <meta charset=\"utf-8\">\n" +
           "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
           "    <title>Code de réinitialisation de votre mot de passe</title>\n" +
           "</head>\n" +
           "<body style=\"margin: 0; padding: 0; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #333; background-color: #f8f9fa;\">\n" +
           "    <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"center\" width=\"100%\" style=\"max-width: 600px; margin: 0 auto; padding: 40px 20px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);\">\n" +
           "        <tr>\n" +
           "            <td style=\"text-align: center; padding-bottom: 30px;\">\n" +
           "                <img src=\"https://zenlife-gs2w.onrender.com/img/logo.png\" alt=\"ZenLife\" width=\"150\" style=\"display: inline-block; max-width: 100%; height: auto;\">\n" +
           "            </td>\n" +
           "        </tr>\n" +
           "        <tr>\n" +
           "            <td style=\"padding: 0 30px;\">\n" +
           "                <h1 style=\"color: #4f46e5; font-size: 24px; font-weight: 600; margin-bottom: 20px;\">Code de réinitialisation de votre mot de passe</h1>\n" +
           "                <p style=\"color: #4b5563; font-size: 16px; line-height: 1.6; margin-bottom: 25px;\">Bonjour " + fullName + ",</p>\n" +
           "                <p style=\"color: #4b5563; font-size: 16px; line-height: 1.6; margin-bottom: 25px;\">Nous avons reçu une demande de réinitialisation de mot de passe pour votre compte ZenLife. Utilisez le code de vérification ci-dessous pour réinitialiser votre mot de passe :</p>\n" +
           "                <div style=\"text-align: center; margin: 40px 0;\">\n" +
           "                    <div style=\"display: inline-block; background-color: #f3f4f6; border: 2px dashed #4f46e5; padding: 20px 30px; border-radius: 8px;\">\n" +
           "                        <span style=\"color: #4f46e5; font-size: 32px; font-weight: 700; letter-spacing: 4px; font-family: 'Courier New', monospace;\">" + resetCode + "</span>\n" +
           "                    </div>\n" +
           "                </div>\n" +
           "                <p style=\"color: #4b5563; font-size: 16px; line-height: 1.6; margin-bottom: 15px;\">Ce code est valable pendant 1 heure. Si vous n'avez pas demandé de réinitialisation de mot de passe, vous pouvez ignorer cet email en toute sécurité.</p>\n" +
           "                <p style=\"color: #ef4444; font-size: 14px; line-height: 1.6; margin-bottom: 30px; background-color: #fef2f2; padding: 12px; border-radius: 4px; border-left: 4px solid #ef4444;\">⚠️ Ne partagez jamais ce code avec quiconque. ZenLife ne vous demandera jamais votre code par téléphone ou email.</p>\n" +
           "            </td>\n" +
           "        </tr>\n" +
           "        <tr>\n" +
           "            <td style=\"padding: 30px; border-top: 1px solid #e5e7eb; text-align: center;\">\n" +
           "                <p style=\"color: #6b7280; font-size: 14px; margin-bottom: 10px;\">Cordialement,</p>\n" +
           "                <p style=\"color: #4f46e5; font-size: 16px; font-weight: 500; margin-bottom: 20px;\">L'équipe ZenLife</p>\n" +
           "                <div style=\"margin-top: 20px;\">\n" +
           "                    <a href=\"#\" style=\"display: inline-block; margin: 0 8px;\"><img src=\"" + frontendUrl + "/assets/img/social/facebook.png\" alt=\"Facebook\" width=\"24\"></a>\n" +
           "                    <a href=\"#\" style=\"display: inline-block; margin: 0 8px;\"><img src=\"" + frontendUrl + "/assets/img/social/twitter.png\" alt=\"Twitter\" width=\"24\"></a>\n" +
           "                    <a href=\"#\" style=\"display: inline-block; margin: 0 8px;\"><img src=\"" + frontendUrl + "/assets/img/social/instagram.png\" alt=\"Instagram\" width=\"24\"></a>\n" +
           "                </div>\n" +
           "            </td>\n" +
           "        </tr>\n" +
           "    </table>\n" +
           "</body>\n" +
           "</html>";
}

    @Transactional
public void sendPasswordResetCode(String email) {
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    // Générer un code de vérification à 8 caractères
    String verificationCode = generateVerificationCode(8);
    
    // Définir la date d'expiration (1 heure)
    Instant expirationTime = Instant.now().plus(1, ChronoUnit.HOURS);
    
    // Sauvegarder le code et l'expiration
    user.setVerificationToken(verificationCode);
    user.setTokenExpirationDate(expirationTime);
    user.setUpdatedAt(Instant.now());
    
    userRepository.save(user);
    
    try {
        sendPasswordResetEmail(user, verificationCode);
    } catch (MessagingException e) {
        throw new RuntimeException("Erreur lors de l'envoi de l'email");
    }
}

@Transactional
public void resetPasswordWithCode(String code, String newPassword) {
    User user = userRepository.findByVerificationToken(code)
            .orElseThrow(() -> new RuntimeException("Code invalide ou expiré"));

    // Vérifier si le token a expiré
    if (user.getTokenExpirationDate() != null && 
        user.getTokenExpirationDate().isBefore(Instant.now())) {
        throw new RuntimeException("Code expiré");
    }

    // Vérifier la validité du nouveau mot de passe
    if (!isPasswordValid(newPassword)) {
        throw new IllegalArgumentException(
                "Le mot de passe doit contenir au moins 8 caractères, une lettre majuscule, une lettre minuscule, un chiffre et un caractère spécial.");
    }

    // Mettre à jour le mot de passe
    user.setPassword(passwordEncoder.encode(newPassword));
    user.setVerificationToken(null);
    user.setTokenExpirationDate(null);
    user.setUpdatedAt(Instant.now());

    userRepository.save(user);
}

public void validateResetCode(String code) {
    User user = userRepository.findByVerificationToken(code)
            .orElseThrow(() -> new RuntimeException("Code invalide ou expiré"));

    // Vérifier si le token a expiré
    if (user.getTokenExpirationDate() != null && 
        user.getTokenExpirationDate().isBefore(Instant.now())) {
        throw new RuntimeException("Code expiré");
    }
}

private String generateVerificationCode(int length) {
    String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    SecureRandom random = new SecureRandom();
    StringBuilder code = new StringBuilder();
    
    for (int i = 0; i < length; i++) {
        code.append(characters.charAt(random.nextInt(characters.length())));
    }
    
    return code.toString();
}
    
    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Token invalide ou expiré"));
        
        // Vérifier la validité du nouveau mot de passe
        if (!isPasswordValid(newPassword)) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins 8 caractères, une lettre majuscule, une lettre minuscule, un chiffre et un caractère spécial.");
        }
        
        // Mettre à jour le mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setVerificationToken(null); // Effacer le token après utilisation
        user.setUpdatedAt(Instant.now());
        
        userRepository.save(user);
    }
    
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        
        // Vérifier le mot de passe actuel
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadCredentialsException("Mot de passe actuel incorrect");
        }
        
        // Vérifier la validité du nouveau mot de passe
        if (!isPasswordValid(newPassword)) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins 8 caractères, une lettre majuscule, une lettre minuscule, un chiffre et un caractère spécial.");
        }
        
        // Mettre à jour le mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());
        
        userRepository.save(user);
    }
    
    @Transactional
    public void deactivateAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        
        user.setEnabled(false);
        user.setUpdatedAt(Instant.now());
        
        userRepository.save(user);
    }
    
    public void sendReactivationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Aucun compte n'existe avec cet email"));
        
        if (user.isEnabled()) {
            throw new RuntimeException("Ce compte est déjà actif");
        }
        
        // Générer un token de réactivation
        String reactivationToken = UUID.randomUUID().toString();
        user.setVerificationToken(reactivationToken);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        
        // Envoyer l'email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailFrom);
        message.setTo(user.getEmail());
        message.setSubject("ZenLife - Réactivation de votre compte");
        message.setText("Bonjour " + user.getFullName() + ",\n\n" +
                "Vous avez demandé la réactivation de votre compte. Veuillez cliquer sur le lien suivant pour réactiver votre compte :\n\n" +
                frontendUrl + "/reactivate-account/" + reactivationToken + "\n\n" +
                "Ce lien est valable pendant 24 heures.\n\n" +
                "Si vous n'avez pas demandé la réactivation de votre compte, vous pouvez ignorer cet email.\n\n" +
                "Cordialement,\n" +
                "L'équipe ZenLife");
        
        mailSender.send(message);
    }
    
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedAccessException("Aucun utilisateur authentifié trouvé");
        }
        
        // Récupérer l'email et charger l'utilisateur depuis la base de données
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + email));
    }
    
    public User updateNotificationPreferences(String email, String preferences) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        
        user.setNotificationPreferences(preferences);
        user.setUpdatedAt(Instant.now());
        
        return userRepository.save(user);
    }
    
    public User updateThemePreference(String email, String themePreference) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        
        user.setThemePreference(themePreference);
        user.setUpdatedAt(Instant.now());
        
        return userRepository.save(user);
    }
}