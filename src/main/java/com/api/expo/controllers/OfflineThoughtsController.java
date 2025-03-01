package com.api.expo.controllers;

import com.api.expo.models.PositiveThought;
import com.api.expo.services.PositiveThoughtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Contrôleur pour fournir des pensées positives par défaut en cas de problème de connexion.
 * Ces pensées peuvent être mises en cache par le service worker.
 */
@RestController
@RequestMapping("/api/offline")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class OfflineThoughtsController {

    private final PositiveThoughtService positiveThoughtService;
    
    @GetMapping("/positive-thoughts/default")
    public ResponseEntity<List<PositiveThought>> getDefaultThoughts() {
        // Récupérer quelques pensées qu'on peut mettre en cache pour le mode hors ligne
        try {
            // Essayer d'abord de récupérer les pensées depuis la base de données
            List<PositiveThought> thoughts = positiveThoughtService.getAllPositiveThoughts();
            
            // Si on a au moins 5 pensées, retourner les 5 premières
            if (thoughts != null && thoughts.size() >= 5) {
                return ResponseEntity.ok(thoughts.subList(0, 5));
            }
            
            // Sinon, créer quelques pensées par défaut
            return ResponseEntity.ok(createDefaultThoughts());
        } catch (Exception e) {
            // En cas d'erreur, retourner des pensées par défaut
            return ResponseEntity.ok(createDefaultThoughts());
        }
    }
    
    private List<PositiveThought> createDefaultThoughts() {
        // Pensées par défaut pour le mode hors ligne
        PositiveThought[] thoughts = {
            createThought("Chaque jour est une nouvelle chance de changer votre vie.", "Anonyme", "Motivation"),
            createThought("Le bonheur n'est pas quelque chose de prêt à l'emploi. Il découle de vos propres actions.", "Dalaï Lama", "Bonheur"),
            createThought("La vie est ce qui arrive pendant que vous êtes occupé à faire d'autres projets.", "John Lennon", "Sagesse"),
            createThought("Croyez en vous-même et en tout ce que vous êtes. Sachez qu'il y a quelque chose en vous qui est plus grand que tout obstacle.", "Christian D. Larson", "Motivation"),
            createThought("Le succès n'est pas final, l'échec n'est pas fatal : c'est le courage de continuer qui compte.", "Winston Churchill", "Succès")
        };
        
        return Arrays.asList(thoughts);
    }
    
    private PositiveThought createThought(String content, String author, String category) {
        PositiveThought thought = new PositiveThought();
        thought.setContent(content);
        thought.setAuthor(author);
        thought.setCategory(category);
        return thought;
    }
}