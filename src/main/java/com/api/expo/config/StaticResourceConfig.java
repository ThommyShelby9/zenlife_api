// Ajoutez cette classe de configuration pour permettre l'accès aux fichiers

package com.api.expo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Exposer le répertoire des téléchargements comme ressource statique
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .setCachePeriod(3600) // Cache pendant 1 heure
                .resourceChain(true);
        
        // Exposer le répertoire des photos de profil
        registry.addResourceHandler("/profile-pictures/**")
                .addResourceLocations("file:" + uploadDir + "/profile_pictures/")
                .setCachePeriod(3600)
                .resourceChain(true);
    }
}