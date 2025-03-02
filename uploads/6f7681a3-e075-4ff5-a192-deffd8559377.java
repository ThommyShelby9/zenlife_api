package com.api.expo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.expo.services.FileStorageService;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Fichiers", description = "API de gestion des fichiers et images")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/images/{fileName}")
    @Operation(
        summary = "Récupération d'une image",
        description = "Permet de récupérer une image à partir de son nom de fichier"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Image récupérée avec succès",
            content = @Content(
                mediaType = "image/*",
                schema = @Schema(type = "string", format = "binary")
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Image non trouvée",
            content = @Content
        )
    })
    public ResponseEntity<Resource> getImage(
        @Parameter(description = "Nom du fichier image à récupérer")
        @PathVariable String fileName
    ) {
        try {
            Resource resource = fileStorageService.loadFileAsResource(fileName);
            
            String contentType = determineContentType(fileName);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
                
        } catch (IOException ex) {
            return ResponseEntity.notFound().build();
        }
    }
    
    private String determineContentType(String fileName) {
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (fileExtension) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            default:
                return "application/octet-stream";
        }
    }
}