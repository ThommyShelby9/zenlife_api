package com.api.expo.services;

public class ValidationNotFoundException extends RuntimeException {

    // Constructeur par défaut
    public ValidationNotFoundException() {
        super("Code de validation non trouvé.");
    }

    // Constructeur avec un message personnalisé
    public ValidationNotFoundException(String message) {
        super(message);
    }
}
