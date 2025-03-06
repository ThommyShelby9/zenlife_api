package com.api.expo.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {
    
    private final Cloudinary cloudinary;
    
    /**
     * Télécharge une image de profil sur Cloudinary
     * @param file le fichier à télécharger
     * @param userId l'ID de l'utilisateur pour créer un nom unique
     * @return l'URL de l'image téléchargée
     * @throws IOException en cas d'erreur lors du téléchargement
     */
    public String uploadProfilePicture(MultipartFile file, String userId) throws IOException {
        try {
            // Ajouter un UUID aléatoire pour éviter les collisions
            String publicId = "profile_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8);
            
            // Version minimale des options - pas de transformation
            Map<String, Object> options = new HashMap<>();
            options.put("public_id", publicId);
            options.put("folder", "profile-pictures");
            
            // Log des options pour diagnostic
            log.info("Options d'upload Cloudinary: {}", options);
            
            // Upload sans transformation
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            
            // Log complet de la réponse
            log.info("Réponse Cloudinary: {}", uploadResult);
            
            // Obtenir l'URL
            String secureUrl = (String) uploadResult.get("secure_url");
            
            // Ajouter les transformations à l'URL manuellement
            int insertPoint = secureUrl.indexOf("/upload/") + 8;
            String transformedUrl = secureUrl.substring(0, insertPoint)
                            + "c_fill,g_face,h_400,w_400/"
                            + secureUrl.substring(insertPoint);
            
            log.info("URL transformée: {}", transformedUrl);
            
            return transformedUrl;
            
        } catch (Exception e) {
            log.error("Erreur détaillée lors de l'upload Cloudinary:", e);
            throw e;
        }
    }
    
    /**
     * Télécharge un fichier générique sur Cloudinary
     * @param file le fichier à télécharger
     * @param options les options d'upload Cloudinary
     * @return la réponse complète de Cloudinary
     * @throws IOException en cas d'erreur lors du téléchargement
     */
    public Map<String, Object> uploadFile(MultipartFile file, Map<String, Object> options) throws IOException {
        try {
            log.info("Upload de fichier vers Cloudinary avec options: {}", options);
            
            // Si aucune option n'est fournie, créer un objet vide
            if (options == null) {
                options = new HashMap<>();
            }
            
            // Si resource_type n'est pas spécifié, définir "auto" pour détecter automatiquement
            if (!options.containsKey("resource_type")) {
                options.put("resource_type", "auto");
            }
            
            // Upload du fichier
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            
            log.info("Réponse Cloudinary pour uploadFile: {}", uploadResult);
            
            return uploadResult;
        } catch (Exception e) {
            log.error("Erreur lors de l'upload de fichier vers Cloudinary:", e);
            throw e;
        }
    }
    
    /**
     * Télécharge un fichier audio sur Cloudinary
     * @param file le fichier audio à télécharger
     * @param messageId l'ID du message pour créer un nom unique
     * @param durationSeconds la durée de l'audio en secondes
     * @return la réponse complète de Cloudinary
     * @throws IOException en cas d'erreur lors du téléchargement
     */
    public Map<String, Object> uploadVoiceNote(MultipartFile file, String messageId, Double durationSeconds) throws IOException {
        try {
            // Créer un identifiant unique pour la note vocale
            String publicId = "voice_note_" + messageId + "_" + UUID.randomUUID().toString().substring(0, 8);
            
            Map<String, Object> options = new HashMap<>();
            options.put("public_id", publicId);
            options.put("folder", "voice-notes");
            options.put("resource_type", "video"); // Pour les fichiers audio
            
            // Ajouter des métadonnées pour la durée
            Map<String, Object> context = new HashMap<>();
            context.put("duration_seconds", durationSeconds.toString());
            options.put("context", context);
            
            log.info("Upload de note vocale vers Cloudinary avec options: {}", options);
            
            // Upload de la note vocale
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            
            log.info("Réponse Cloudinary pour uploadVoiceNote: {}", uploadResult);
            
            return uploadResult;
        } catch (Exception e) {
            log.error("Erreur lors de l'upload de note vocale vers Cloudinary:", e);
            throw e;
        }
    }
    
    /**
     * Supprime un fichier de Cloudinary
     * @param publicId l'identifiant public ou l'URL du fichier à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    public boolean deleteFile(String publicId) {
        try {
            // Extraire le public_id à partir de l'URL ou du chemin complet
            if (publicId.startsWith("http") && publicId.contains("/upload/")) {
                // Format: https://res.cloudinary.com/cloud_name/image/upload/v1234567890/folder/public_id.ext
                String afterUpload = publicId.split("/upload/")[1];
                
                // Enlever la version (v1234567890/) si présente
                if (afterUpload.startsWith("v")) {
                    int nextSlash = afterUpload.indexOf("/");
                    if (nextSlash > 0) {
                        afterUpload = afterUpload.substring(nextSlash + 1);
                    }
                }
                
                // Enlever l'extension
                if (afterUpload.contains(".")) {
                    afterUpload = afterUpload.substring(0, afterUpload.lastIndexOf('.'));
                }
                
                publicId = afterUpload;
            }
            
            log.info("Suppression du fichier Cloudinary: {}", publicId);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            
            log.info("Résultat de la suppression: {}", result);
            
            return "ok".equals(result.get("result"));
        } catch (IOException e) {
            log.error("Erreur lors de la suppression du fichier Cloudinary", e);
            return false;
        }
    }
}