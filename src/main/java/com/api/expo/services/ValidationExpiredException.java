package com.api.expo.services;

public class ValidationExpiredException extends RuntimeException {

    // Constructeur par défaut
    public ValidationExpiredException() {
        super("Le code de validation a expiré.");
    }

    // Constructeur avec un message personnalisé
    public ValidationExpiredException(String message) {
        super(message);
    }
}
