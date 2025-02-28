package com.api.expo.services;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {
    
    public String getVerificationEmailContent(String firstName, String code, String frontendUrl, int expirationHours) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 5px;">
                <h2 style="color: #4CAF50;">Vérification de votre adresse email</h2>
                <p>Bonjour %s,</p>
                <p>Merci de vous être inscrit sur ZenLife. Pour compléter votre inscription, veuillez vérifier votre adresse email en cliquant sur le lien ci-dessous :</p>
                <p style="text-align: center;">
                    <a href="%s/verify?code=%s" style="display: inline-block; padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px;">Vérifier mon adresse email</a>
                </p>
                <p>Ou utilisez ce code de vérification : <strong>%s</strong></p>
                <p>Ce lien est valable pendant %d heures.</p>
                <p>Si vous n'avez pas créé de compte sur ZenLife, vous pouvez ignorer cet email.</p>
                <p>Cordialement,<br>L'équipe ZenLife</p>
            </div>
            """.formatted(firstName, frontendUrl, code, code, expirationHours);
    }
    
    public String getPasswordResetContent(String firstName, String resetCode, String frontendUrl) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 5px;">
                <h2 style="color: #4CAF50;">Réinitialisation de votre mot de passe</h2>
                <p>Bonjour %s,</p>
                <p>Vous avez demandé la réinitialisation de votre mot de passe. Veuillez cliquer sur le lien ci-dessous pour définir un nouveau mot de passe :</p>
                <p style="text-align: center;">
                    <a href="%s/reset-password?token=%s" style="display: inline-block; padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px;">Réinitialiser mon mot de passe</a>
                </p>
                <p>Ou utilisez ce code de réinitialisation : <strong>%s</strong></p>
                <p>Si vous n'avez pas demandé la réinitialisation de votre mot de passe, vous pouvez ignorer cet email.</p>
                <p>Cordialement,<br>L'équipe ZenLife</p>
            </div>
            """.formatted(firstName, frontendUrl, resetCode, resetCode);
    }
}