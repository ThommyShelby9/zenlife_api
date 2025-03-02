// FileController.java
package com.api.expo.controllers;

import com.api.expo.models.FileAttachment;
import com.api.expo.models.VoiceNote;
import com.api.expo.services.FileService;
import lombok.RequiredArgsConstructor;
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
public class FileController {
    
    private final FileService fileService;
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("messageId") String messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            FileAttachment attachment = fileService.storeFile(file, messageId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Fichier téléchargé avec succès");
            response.put("attachment", attachment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
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
            @RequestParam("durationSeconds") Integer durationSeconds,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            VoiceNote voiceNote = fileService.storeVoiceNote(file, messageId, durationSeconds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Note vocale téléchargée avec succès");
            response.put("voiceNote", voiceNote);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
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
            String filename = fileService.storeProfilePicture(userDetails, file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Photo de profil téléchargée avec succès");
            response.put("filename", filename);
            response.put("url", "/api/files/profile-pictures/" + filename);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
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
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erreur lors de la récupération des pièces jointes");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            Resource resource = fileService.loadFileAsResource(filename);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/view/{filename:.+}")
    public ResponseEntity<Resource> viewFile(@PathVariable String filename, @RequestParam(required = false) String token) {
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
            
            Resource resource = fileService.loadFileAsResource(filename);
            
            // Déterminer le type de contenu
            String contentType = "application/octet-stream";
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (filename.endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (filename.endsWith(".mp3")) {
                contentType = "audio/mpeg";
            } else if (filename.endsWith(".webm")) {
                contentType = "audio/webm";
            } else if (filename.endsWith(".ogg")) {
                contentType = "audio/ogg";
            }
            
            // Headers CORS explicites
            return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
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
public ResponseEntity<Resource> getProfilePicture(@PathVariable String filename) {
    try {
        // Charger le fichier depuis le dossier des photos de profil
        Resource resource = fileService.loadProfilePictureAsResource(filename);
        
        // Déterminer le type de contenu
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
        return ResponseEntity.notFound().build();
    }
}
}
