
// 3. Créez le service CloudinaryService
package com.api.expo.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
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
     * Supprime une image de profil de Cloudinary
     * @param publicId l'identifiant public de l'image à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    public boolean deleteProfilePicture(String publicId) {
        try {
            // Extraire le public_id à partir de l'URL ou du chemin complet
            if (publicId.contains("/")) {
                String[] parts = publicId.split("/");
                String fileName = parts[parts.length - 1];
                if (fileName.contains(".")) {
                    fileName = fileName.substring(0, fileName.lastIndexOf('.'));
                }
                publicId = "profile-pictures/" + fileName;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return "ok".equals(result.get("result"));
        } catch (IOException e) {
            log.error("Erreur lors de la suppression de l'image Cloudinary", e);
            return false;
        }
    }
}