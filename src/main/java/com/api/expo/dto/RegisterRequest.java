package com.api.expo.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String fullName;
    private String firstName;  // Modifié de "firstname" à "firstName"
    private String lastName;   // Modifié de "lastname" à "lastName"
    private String username;
    private String email;
    private String password;
    
    // Si vous utilisez Lombok avec @Data, vous n'avez pas besoin d'écrire les getters/setters
    // Sinon, ajoutez les getters/setters suivants pour garantir la compatibilité
    
    /*
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    */
}