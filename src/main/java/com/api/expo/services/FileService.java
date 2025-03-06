package com.api.expo.services;

import com.api.expo.models.ChatMessage;
import com.api.expo.models.FileAttachment;
import com.api.expo.models.User;
import com.api.expo.models.VoiceNote;
import com.api.expo.repository.ChatMessageRepository;
import com.api.expo.repository.FileAttachmentRepository;
import com.api.expo.repository.UserRepository;
import com.api.expo.repository.VoiceNoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {
    
    private final FileAttachmentRepository fileAttachmentRepository;
    private final VoiceNoteRepository voiceNoteRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    @Value("${app.upload.dir}")
    private String uploadDir;
    
    /**
     * Stocke une pièce jointe via Cloudinary
     */
   /**
 * Stocke une pièce jointe via Cloudinary
 */
public Map<String, Object> storeFile(MultipartFile file, String messageId) throws IOException {
    ChatMessage message = chatMessageRepository.findById(messageId)
        .orElseThrow(() -> new RuntimeException("Message non trouvé"));
        
    String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
    
    // Créer un identifiant unique pour le fichier
    String publicId = "attachment_" + UUID.randomUUID().toString();
    
    // Préparer les options pour Cloudinary
    Map<String, Object> options = new HashMap<>();
    options.put("public_id", publicId);
    options.put("folder", "chat-attachments");
    options.put("resource_type", "auto"); // Pour accepter tous types de fichiers
    
    // Upload vers Cloudinary
    Map<String, Object> uploadResult = cloudinaryService.uploadFile(file, options);
    
    // Obtenir l'URL sécurisée
    String secureUrl = (String) uploadResult.get("secure_url");
    
    // Créer l'entité FileAttachment
    FileAttachment attachment = new FileAttachment();
    attachment.setMessage(message);
    attachment.setFilename(originalFilename);
    attachment.setContentType(file.getContentType());
    attachment.setFileSize(file.getSize());
    attachment.setStoragePath(secureUrl); // Stocker l'URL Cloudinary directement
    attachment.setCreatedAt(Instant.now());
    
    FileAttachment savedAttachment = fileAttachmentRepository.save(attachment);
    
    // Créer et retourner une map avec les informations de la pièce jointe
    Map<String, Object> response = new HashMap<>();
    response.put("id", savedAttachment.getId());
    response.put("filename", savedAttachment.getFilename());
    response.put("contentType", savedAttachment.getContentType());
    response.put("size", savedAttachment.getFileSize());
    response.put("url", secureUrl);
    
    log.info("Fichier téléchargé sur Cloudinary: {}", secureUrl);
    
    return response;
}
    
    /**
     * Stocke une note vocale via Cloudinary
     */
    public Map<String, Object> saveVoiceNote(MultipartFile file, String messageId, Double durationSeconds) throws IOException {
        ChatMessage message = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message non trouvé"));
        
        // Créer un identifiant unique pour la note vocale
        String publicId = "voice_note_" + UUID.randomUUID().toString();
        
        // Préparer les options pour Cloudinary
        Map<String, Object> options = new HashMap<>();
        options.put("public_id", publicId);
        options.put("folder", "voice-notes");
        options.put("resource_type", "video"); // Pour les fichiers audio
        
        // Upload vers Cloudinary
        Map<String, Object> uploadResult = cloudinaryService.uploadFile(file, options);
        
        // Obtenir l'URL sécurisée
        String secureUrl = (String) uploadResult.get("secure_url");
        
        // Créer d'abord la pièce jointe standard
        FileAttachment attachment = new FileAttachment();
        attachment.setMessage(message);
        attachment.setFilename("voice-note-" + UUID.randomUUID().toString() + ".webm");
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setStoragePath(secureUrl);
        attachment.setCreatedAt(Instant.now());
        
        FileAttachment savedAttachment = fileAttachmentRepository.save(attachment);
        
        // Puis créer la note vocale avec la durée
        VoiceNote voiceNote = new VoiceNote();
        voiceNote.setMessage(message);
        voiceNote.setDurationSeconds(durationSeconds);
        voiceNote.setStoragePath(secureUrl);
        voiceNote.setCreatedAt(Instant.now());
        voiceNote.setFilename(attachment.getFilename());
        voiceNote.setContentType(file.getContentType());
        voiceNote.setFileSize(file.getSize());
        
        VoiceNote savedVoiceNote = voiceNoteRepository.save(voiceNote);
        
        // Créer la réponse
        Map<String, Object> response = new HashMap<>();
        response.put("id", savedAttachment.getId());
        response.put("filename", savedAttachment.getFilename());
        response.put("contentType", savedAttachment.getContentType());
        response.put("size", savedAttachment.getFileSize());
        response.put("url", secureUrl);
        response.put("durationSeconds", savedVoiceNote.getDurationSeconds());
        response.put("isVoiceNote", true);
        
        log.info("Note vocale téléchargée sur Cloudinary: {}", secureUrl);
        
        return response;
    }
    
    /**
     * Récupère les pièces jointes d'un message
     */
    public List<FileAttachment> getMessageAttachments(String messageId) {
        return fileAttachmentRepository.findByMessageId(messageId);
    }
    
    /**
     * Stocke l'image de profil d'un utilisateur dans Cloudinary
     * (existant, inchangé)
     */
    public String storeProfilePicture(UserDetails userDetails, MultipartFile file) throws IOException {
        try {
             User user = userRepository.findByEmail(userDetails.getUsername())
             .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            // Supprimer l'ancienne image si elle existe et n'est pas une URL complète
            String oldProfilePicture = user.getProfilePicture();
            if (oldProfilePicture != null && !oldProfilePicture.startsWith("http")) {
                // L'ancienne image n'était pas sur Cloudinary, pas besoin de la supprimer
            }
            
            // Télécharger la nouvelle image
            String imageUrl = cloudinaryService.uploadProfilePicture(file, user.getId());
            
            // Mettre à jour l'utilisateur avec l'URL complète
            user.setProfilePicture(imageUrl);
            user.setProfilePictureUrl(imageUrl);

            userRepository.save(user);
            
            return imageUrl;
        } catch (IOException e) {
         throw new RuntimeException("Erreur lors du stockage de l'image de profil", e);
        }
    }
    
    /**
     * Méthode de compatibilité pour garantir que les anciennes références aux fichiers
     * stockés localement fonctionnent toujours, mais rediriger vers Cloudinary si possible
     */
    public Resource loadFileAsResource(String filename) throws IOException {
        try {
            // Vérifier d'abord si c'est une URL Cloudinary
            if (filename.startsWith("http")) {
                // C'est une URL Cloudinary, pas besoin de charger un fichier local
                return null; // On ne peut pas retourner un Resource pour une URL distante
            }
            
            // Sinon, rechercher d'abord le fichier dans la base de données
            FileAttachment attachment = fileAttachmentRepository.findByFilename(filename)
                .orElse(null);
                
            if (attachment != null && attachment.getStoragePath().startsWith("http")) {
                // Le fichier est sur Cloudinary, on ne peut pas retourner un Resource
                return null; // Le contrôleur devra rediriger vers l'URL Cloudinary
            }
            
            // Alors c'est un ancien fichier local
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return resource;
            }
            
            // Si non trouvé, chercher dans les sous-répertoires
            String[] possibleSubdirs = {"attachments", "voice_notes"};
            for (String subdir : possibleSubdirs) {
                Path subdirPath = Paths.get(uploadDir, subdir).resolve(filename).normalize();
                Resource subdirResource = new UrlResource(subdirPath.toUri());
                if (subdirResource.exists()) {
                    return subdirResource;
                }
            }
            
            throw new RuntimeException("Fichier non trouvé: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement du fichier: " + filename, e);
        }
    }
    
    /**
     * Vérifie si un fichier est stocké sur Cloudinary
     */
    public boolean isCloudinaryFile(String filename) {
        // Si c'est une URL, c'est sur Cloudinary
        if (filename.startsWith("http")) {
            return true;
        }
        
        // Vérifier dans la base de données
        FileAttachment attachment = fileAttachmentRepository.findByFilename(filename)
            .orElse(null);
            
        return attachment != null && attachment.getStoragePath().startsWith("http");
    }
    
    /**
     * Récupère l'URL Cloudinary pour un fichier
     */
    public String getCloudinaryUrl(String filename) {
        // Si c'est déjà une URL, la retourner directement
        if (filename.startsWith("http")) {
            return filename;
        }
        
        // Chercher dans la base de données
        FileAttachment attachment = fileAttachmentRepository.findByFilename(filename)
            .orElse(null);
            
        if (attachment != null && attachment.getStoragePath().startsWith("http")) {
            return attachment.getStoragePath();
        }
        
        // Sinon, c'est un fichier local, pas d'URL Cloudinary
        return null;
    }

}