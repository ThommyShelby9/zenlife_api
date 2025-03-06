package com.api.expo.controllers;

import com.api.expo.models.FileAttachment;
import com.api.expo.services.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class FileController {
    
    private final FileService fileService;
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("messageId") String messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // La méthode storeFile retourne maintenant Map<String, Object>
            Map<String, Object> attachmentInfo = fileService.storeFile(file, messageId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Fichier téléchargé avec succès");
            
            // On peut directement ajouter les infos de l'attachement
            response.putAll(attachmentInfo);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors du téléchargement du fichier", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors du téléchargement du fichier");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    @PostMapping("/voice-note")
    public ResponseEntity<?> uploadVoiceNote(
            @RequestParam("file") MultipartFile file,
            @RequestParam("messageId") String messageId,
            @RequestParam("durationSeconds") Double durationSeconds,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Map<String, Object> voiceNoteInfo = fileService.saveVoiceNote(file, messageId, durationSeconds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Note vocale téléchargée avec succès");
            response.put("voiceNote", voiceNoteInfo);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors du téléchargement de la note vocale", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors du téléchargement de la note vocale");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/profile-picture")
    public ResponseEntity<?> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String imageUrl = fileService.storeProfilePicture(userDetails, file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Photo de profil téléchargée avec succès");
            response.put("url", imageUrl);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors du téléchargement de la photo de profil", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors du téléchargement de la photo de profil");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/attachments/{messageId}")
    public ResponseEntity<?> getMessageAttachments(
            @PathVariable String messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<FileAttachment> attachments = fileService.getMessageAttachments(messageId);
            return ResponseEntity.ok(attachments);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des pièces jointes", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération des pièces jointes");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<?> downloadFile(@PathVariable String filename) {
        try {
            // Vérifier d'abord si c'est un fichier Cloudinary
            if (fileService.isCloudinaryFile(filename)) {
                String cloudinaryUrl = fileService.getCloudinaryUrl(filename);
                
                // Rediriger vers l'URL Cloudinary
                return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, cloudinaryUrl)
                    .build();
            }
            
            // Sinon, c'est un fichier local (ancien système)
            Resource resource = fileService.loadFileAsResource(filename);
            
            if (resource == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
        } catch (Exception e) {
            log.error("Erreur lors du téléchargement du fichier", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/view/{filename:.+}")
    public ResponseEntity<?> viewFile(@PathVariable String filename, @RequestParam(required = false) String token) {
        try {
            // Si un token est fourni, vérifier son authenticité
            if (token != null && !token.isEmpty()) {
                try {
                    // Vérifier le token JWT ici (dépendant de votre implémentation)
                    // Par exemple: jwtService.validateToken(token);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
            }
            
            // Vérifier d'abord si c'est un fichier Cloudinary
            if (fileService.isCloudinaryFile(filename)) {
                String cloudinaryUrl = fileService.getCloudinaryUrl(filename);
                
                // Rediriger vers l'URL Cloudinary
                return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, cloudinaryUrl)
                    .build();
            }
            
            // Sinon, c'est un fichier local (ancien système)
            Resource resource = fileService.loadFileAsResource(filename);
            
            if (resource == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Déterminer le type de contenu
            String contentType = determineContentType(filename);
            
            // Headers CORS explicites
            return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
        } catch (Exception e) {
            log.error("Erreur lors de l'affichage du fichier", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Détermine le type MIME en fonction de l'extension du fichier
     */
    private String determineContentType(String filename) {
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filename.endsWith(".png")) {
            return "image/png";
        } else if (filename.endsWith(".pdf")) {
            return "application/pdf";
        } else if (filename.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (filename.endsWith(".webm")) {
            return "audio/webm";
        } else if (filename.endsWith(".ogg")) {
            return "audio/ogg";
        } else {
            return "application/octet-stream";
        }
    }
    
    // Méthode d'options pour gérer les requêtes préflight CORS
    @RequestMapping(value = "/view/{filename:.+}", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok()
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "GET, OPTIONS")
            .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
            .header("Access-Control-Max-Age", "3600")
            .build();
    }

    @GetMapping("/profile-pictures/{filename:.+}")
    public ResponseEntity<?> getProfilePicture(@PathVariable String filename) {
        try {
            // Les photos de profil sont déjà gérées par Cloudinary
            // Si c'est une URL Cloudinary, rediriger vers cette URL
            if (filename.startsWith("http")) {
                return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, filename)
                    .build();
            }
            
            // Pour les anciennes photos de profil stockées localement
            try {
                Resource resource = fileService.loadFileAsResource(filename);
                
                String contentType = "image/jpeg"; // Par défaut
                if (filename.endsWith(".png")) {
                    contentType = "image/png";
                } else if (filename.endsWith(".gif")) {
                    contentType = "image/gif";
                }
                
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
            } catch (Exception e) {
                log.warn("Photo de profil locale introuvable: {}", filename);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la photo de profil", e);
            return ResponseEntity.notFound().build();
        }
    }
}