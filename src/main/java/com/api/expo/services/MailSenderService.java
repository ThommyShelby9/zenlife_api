package com.api.expo.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.api.expo.models.User;
import com.api.expo.models.Validation;

@Service
public class MailSenderService {
    
    private static final String SENDER_EMAIL = "creatiswap@gmail.com";
    private static final String VERIFICATION_SUBJECT = "Confirmez votre compte ZenLife";
    private static final String RESET_PASSWORD_SUBJECT = "Modification de mot de passe ZenLife";
    
    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    private final JavaMailSender mailSender;
    
    // Constructeur avec injection de JavaMailSender uniquement
    public MailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    public void sendValidationEmail(Validation validation) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(SENDER_EMAIL);
        message.setTo(validation.getUser().getEmail());
        message.setSubject(VERIFICATION_SUBJECT);
        message.setText("Bonjour " + validation.getUser().getFirstname() + ",\n\n" +
                "Merci de vous être inscrit sur ZenLife. Pour compléter votre inscription, " +
                "veuillez vérifier votre adresse email en utilisant ce code : " + validation.getCode() + "\n\n" +
                "Ou en cliquant sur ce lien : " + frontendUrl + "/verify?code=" + validation.getCode() + "\n\n" +
                "Cordialement,\nL'équipe ZenLife");
                
        mailSender.send(message);
    }
    
    public void sendResetEmail(User user, String resetCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(SENDER_EMAIL);
        message.setTo(user.getEmail());
        message.setSubject(RESET_PASSWORD_SUBJECT);
        message.setText("Bonjour " + user.getFirstname() + ",\n\n" +
                "Vous avez demandé la réinitialisation de votre mot de passe. " +
                "Veuillez utiliser ce code : " + resetCode + "\n\n" +
                "Ou en cliquant sur ce lien : " + frontendUrl + "/reset-password?token=" + resetCode + "\n\n" +
                "Cordialement,\nL'équipe ZenLife");
                
        mailSender.send(message);
    }
}