package com.api.expo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Permettre l'accès aux images et fichiers sans authentification
        registry.addResourceHandler("/api/files/images/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .setCachePeriod(3600);  // 1 heure en secondes
                
        // Accès spécifique aux photos de profil 
        registry.addResourceHandler("/api/files/profile-pictures/**")
                .addResourceLocations("file:" + uploadDir + "/profile_pictures/")
                .setCachePeriod(3600);
    }
}