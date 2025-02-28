// FileService.java
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {
    
    private final FileAttachmentRepository fileAttachmentRepository;
    private final VoiceNoteRepository voiceNoteRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    
    @Value("${app.upload.dir}")  // Au lieu de file.upload-dir
    private String uploadDir;
    
    // Gestion des pièces jointes
    public FileAttachment storeFile(MultipartFile file, String messageId) throws IOException {
        ChatMessage message = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message non trouvé"));
            
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        
        if (originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        
        // Créer le répertoire s'il n'existe pas
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Stocker le fichier
        Path targetLocation = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        // Créer l'entité FileAttachment
        FileAttachment attachment = new FileAttachment();
        attachment.setMessage(message);
        attachment.setFilename(originalFilename);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setStoragePath(uniqueFilename);
        attachment.setCreatedAt(Instant.now());
        
        return fileAttachmentRepository.save(attachment);
    }
    
    public List<FileAttachment> getMessageAttachments(String messageId) {
        return fileAttachmentRepository.findByMessageId(messageId);
    }
    
    public Resource loadFileAsResource(String filename) throws MalformedURLException {
        Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        
        if (resource.exists()) {
            return resource;
        } else {
            throw new RuntimeException("Fichier non trouvé: " + filename);
        }
    }
    
    // Gestion des notes vocales
    public VoiceNote storeVoiceNote(MultipartFile file, String messageId, Integer durationSeconds) throws IOException {
        ChatMessage message = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message non trouvé"));
            
        String uniqueFilename = UUID.randomUUID().toString() + ".ogg";
        
        // Créer le répertoire s'il n'existe pas
        Path uploadPath = Paths.get(uploadDir, "voice_notes");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Stocker le fichier
        Path targetLocation = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        // Créer l'entité VoiceNote
        VoiceNote voiceNote = new VoiceNote();
        voiceNote.setMessage(message);
        voiceNote.setDurationSeconds(durationSeconds);
        voiceNote.setStoragePath(uniqueFilename);
        voiceNote.setCreatedAt(Instant.now());
        
        return voiceNoteRepository.save(voiceNote);
    }
    
    // Gestion des photos de profil
    public String storeProfilePicture(UserDetails userDetails, MultipartFile file) throws IOException {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        String fileExtension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String uniqueFilename = "profile_" + user.getId() + "." + fileExtension;
        
        // Créer le répertoire s'il n'existe pas
        Path uploadPath = Paths.get(uploadDir, "profile_pictures");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Stocker le fichier
        Path targetLocation = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        // Mettre à jour l'utilisateur
        user.setProfilePicture(uniqueFilename);
        userRepository.save(user);
        
        return uniqueFilename;
    }

    // Ajout dans FileService.java
public Resource loadProfilePictureAsResource(String filename) throws IOException {
    try {
        Path filePath = Paths.get(uploadDir).resolve("profile_pictures").resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        
        if (resource.exists()) {
            return resource;
        } else {
            throw new RuntimeException("Fichier introuvable: " + filename);
        }
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors du chargement du fichier: " + filename, e);
    }
}
}
