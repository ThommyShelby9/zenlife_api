package com.api.expo.services;

import com.api.expo.models.User;
import com.api.expo.models.WaterIntake;
import com.api.expo.models.WaterReminderSetting;
import com.api.expo.repository.UserRepository;
import com.api.expo.repository.WaterIntakeRepository;
import com.api.expo.repository.WaterReminderSettingRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaterReminderService {
    
    private final WaterReminderSettingRepository waterReminderSettingRepository;
    private final WaterIntakeRepository waterIntakeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    public WaterReminderSetting getUserSettings(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        Optional<WaterReminderSetting> settings = waterReminderSettingRepository.findByUserId(user.getId());
        if (settings.isPresent()) {
            return settings.get();
        } else {
            // Créer des paramètres par défaut si inexistants
            WaterReminderSetting defaultSettings = new WaterReminderSetting();
            defaultSettings.setUser(user);
            defaultSettings.setDailyGoalML(user.getDailyWaterGoalML() != null ? user.getDailyWaterGoalML() : 2000);
            defaultSettings.setReminderIntervalMinutes(60); // Rappel toutes les heures par défaut
            defaultSettings.setEnabled(true);
            
            // Définir les heures de début et de fin par défaut (8h00 à 22h00)
            Instant now = Instant.now();
            Instant startTime = now.truncatedTo(ChronoUnit.DAYS).plus(8, ChronoUnit.HOURS);
            Instant endTime = now.truncatedTo(ChronoUnit.DAYS).plus(22, ChronoUnit.HOURS);
            
            defaultSettings.setStartTime(startTime);
            defaultSettings.setEndTime(endTime);
            
            return waterReminderSettingRepository.save(defaultSettings);
        }
    }
    
    public WaterReminderSetting updateUserSettings(UserDetails userDetails, WaterReminderSetting updatedSettings) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        Optional<WaterReminderSetting> existingSettings = waterReminderSettingRepository.findByUserId(user.getId());
        
        WaterReminderSetting settingsToSave;
        if (existingSettings.isPresent()) {
            settingsToSave = existingSettings.get();
            settingsToSave.setDailyGoalML(updatedSettings.getDailyGoalML());
            settingsToSave.setReminderIntervalMinutes(updatedSettings.getReminderIntervalMinutes());
            settingsToSave.setEnabled(updatedSettings.getEnabled());
            settingsToSave.setStartTime(updatedSettings.getStartTime());
            settingsToSave.setEndTime(updatedSettings.getEndTime());
            settingsToSave.setUpdatedAt(Instant.now());
            
            // Enregistrer les tailles de verre personnalisées
            if (updatedSettings.getCustomGlassSizes() != null) {
                settingsToSave.setCustomGlassSizes(updatedSettings.getCustomGlassSizes());
            }
        } else {
            settingsToSave = updatedSettings;
            settingsToSave.setUser(user);
            settingsToSave.setCreatedAt(Instant.now());
            settingsToSave.setUpdatedAt(Instant.now());
        }
        
        // Mettre également à jour l'objectif quotidien d'eau dans le profil utilisateur
        user.setDailyWaterGoalML(updatedSettings.getDailyGoalML());
        userRepository.save(user);
        
        return waterReminderSettingRepository.save(settingsToSave);
    }
    
    public WaterIntake logWaterIntake(UserDetails userDetails, Integer quantityML) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        WaterIntake intake = new WaterIntake();
        intake.setUser(user);
        intake.setQuantityML(quantityML);
        intake.setIntakeTime(Instant.now());
        
        WaterIntake savedIntake = waterIntakeRepository.save(intake);
        
        // Vérifier la progression après l'ajout
        Map<String, Object> progress = getDailyProgress(userDetails);
        int percentage = (int) progress.get("percentage");
        
        // Envoyer une notification de progression si certains seuils sont atteints
        if (percentage == 50 || percentage == 75 || percentage >= 100) {
            notificationService.createWaterProgressNotification(user, percentage);
        }
        
        return savedIntake;
    }
    
    public List<WaterIntake> getUserIntakeHistory(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        return waterIntakeRepository.findByUserIdOrderByIntakeTimeDesc(user.getId());
    }
    
    public Map<String, Object> getDailyProgress(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        return getDailyProgressForUser(user);
    }
    
    private Map<String, Object> getDailyProgressForUser(User user) {
        // Calculer le début et la fin de la journée
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        // Obtenir l'objectif quotidien
        Integer dailyGoal = user.getDailyWaterGoalML();
        if (dailyGoal == null) {
            Optional<WaterReminderSetting> settings = waterReminderSettingRepository.findByUserId(user.getId());
            dailyGoal = settings.map(WaterReminderSetting::getDailyGoalML).orElse(2000);
        }
        
        // Obtenir la consommation totale d'aujourd'hui
        Integer totalIntake = waterIntakeRepository.getTotalIntakeForUserInRange(user.getId(), startOfDay, endOfDay);
        if (totalIntake == null) {
            totalIntake = 0;
        }
        
        // Calculer le pourcentage d'avancement
        int percentage = (int) Math.min(100, (totalIntake * 100.0) / dailyGoal);
        
        Map<String, Object> result = new HashMap<>();
        result.put("dailyGoalML", dailyGoal);
        result.put("currentIntakeML", totalIntake);
        result.put("remainingML", Math.max(0, dailyGoal - totalIntake));
        result.put("percentage", percentage);
        
        // Ajouter aussi les champs utilisés par le frontend pour assurer la compatibilité
        result.put("dailyGoal", dailyGoal);
        result.put("currentIntake", totalIntake);
        
        return result;
    }
    
    public void sendWaterReminder(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        Optional<WaterReminderSetting> settingsOpt = waterReminderSettingRepository.findByUserId(userId);
        if (settingsOpt.isPresent() && settingsOpt.get().getEnabled()) {
            // Vérifier les préférences de notification de l'utilisateur
            String notificationPrefs = user.getNotificationPreferences();
            if (notificationPrefs != null && 
                (notificationPrefs.equals("ALL") || notificationPrefs.equals("WATER_ONLY"))) {
                
                try {
                    // Formater le message en fonction de la progression
                    Map<String, Object> progress = getDailyProgressForUser(user);
                    int percentage = (int) progress.get("percentage");
                    int remainingML = (int) progress.get("remainingML");
                    
                    String message;
                    if (percentage < 25) {
                        message = "Il est temps de boire de l'eau! Vous n'avez consommé que " + percentage + 
                                 "% de votre objectif aujourd'hui. 💧";
                    } else if (percentage < 50) {
                        message = "Rappel d'hydratation: Il vous reste " + (remainingML / 1000.0) + 
                                 "L à boire pour atteindre votre objectif. 💦";
                    } else if (percentage < 75) {
                        message = "Continuez sur votre lancée! Encore " + (remainingML / 1000.0) + 
                                 "L pour atteindre votre objectif d'hydratation. 💧";
                    } else if (percentage < 100) {
                        message = "Vous y êtes presque! Encore un petit effort de " + (remainingML / 1000.0) + 
                                 "L pour atteindre votre objectif. 💦";
                    } else {
                        message = "Bravo, vous avez atteint votre objectif d'hydratation aujourd'hui! 🎉💧";
                    }
                    
                    notificationService.sendWaterReminderNotification(user, message);
                    
                    // Enregistrer l'envoi du rappel dans les logs
                    log.info("Rappel d'hydratation envoyé à l'utilisateur: {}", user.getUsername());
                } catch (Exception e) {
                    log.error("Erreur lors de l'envoi du rappel d'hydratation: {}", e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * Vérifie si l'utilisateur a atteint son objectif d'hydratation aujourd'hui
     */
    public boolean hasReachedDailyGoal(String userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                
            // Calculer le début et la fin de la journée
            Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant endOfDay = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            
            // Obtenir l'objectif quotidien
            Integer dailyGoal = user.getDailyWaterGoalML();
            if (dailyGoal == null) {
                Optional<WaterReminderSetting> settings = waterReminderSettingRepository.findByUserId(userId);
                dailyGoal = settings.map(WaterReminderSetting::getDailyGoalML).orElse(2000);
            }
            
            // Obtenir la consommation totale d'aujourd'hui
            Integer totalIntake = waterIntakeRepository.getTotalIntakeForUserInRange(userId, startOfDay, endOfDay);
            if (totalIntake == null) {
                return false;
            }
            
            return totalIntake >= dailyGoal;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de l'objectif d'hydratation: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Exporte les données d'hydratation au format CSV
     */
    public byte[] exportDataToCsv(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        List<WaterIntake> history = waterIntakeRepository.findByUserIdOrderByIntakeTimeDesc(user.getId());
        
        StringBuilder csv = new StringBuilder();
        
        // Entête CSV
        csv.append("Date,Heure,Quantité (ml),Quantité (L)\n");
        
        // Lignes de données
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        
        for (WaterIntake intake : history) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(intake.getIntakeTime(), ZoneId.systemDefault());
            
            csv.append(dateTime.format(dateFormatter))
               .append(",")
               .append(dateTime.format(timeFormatter))
               .append(",")
               .append(intake.getQuantityML())
               .append(",")
               .append(String.format("%.2f", intake.getQuantityML() / 1000.0))
               .append("\n");
        }
        
        return csv.toString().getBytes();
    }
    
    /**
     * Exporte les données d'hydratation au format Excel
     */
    public byte[] exportDataToExcel(UserDetails userDetails) throws Exception {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        List<WaterIntake> history = waterIntakeRepository.findByUserIdOrderByIntakeTimeDesc(user.getId());
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Historique d'hydratation");
            
            // Styles pour l'entête
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            XSSFFont font = ((XSSFWorkbook) workbook).createFont();
            font.setFontName("Arial");
            font.setFontHeightInPoints((short) 12);
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            
            // Créer l'entête
            String[] headers = {"Date", "Heure", "Quantité (ml)", "Quantité (L)"};
            Row headerRow = sheet.createRow(0);
            
            for (int col = 0; col < headers.length; col++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(col, 15 * 256); // 15 caractères de largeur
            }
            
            // Style pour les lignes alternées
            CellStyle evenRowStyle = workbook.createCellStyle();
            evenRowStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            evenRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // Ajouter les données
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            
            for (int i = 0; i < history.size(); i++) {
                WaterIntake intake = history.get(i);
                Row row = sheet.createRow(i + 1);
                
                // Appliquer le style aux lignes paires
                if (i % 2 == 0) {
                    for (int j = 0; j < headers.length; j++) {
                        row.createCell(j).setCellStyle(evenRowStyle);
                    }
                }
                
                LocalDateTime dateTime = LocalDateTime.ofInstant(intake.getIntakeTime(), ZoneId.systemDefault());
                
                row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(dateTime.format(dateFormatter));
                row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(dateTime.format(timeFormatter));
                row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(intake.getQuantityML());
                row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(intake.getQuantityML() / 1000.0);
            }
            
            // Ajouter une ligne récapitulative
            int totalRow = history.size() + 2;
            Row totalRowObj = sheet.createRow(totalRow);
            
            CellStyle totalStyle = workbook.createCellStyle();
            totalStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            XSSFFont totalFont = ((XSSFWorkbook) workbook).createFont();
            totalFont.setFontName("Arial");
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);
            
            for (int i = 0; i < 4; i++) {
                totalRowObj.createCell(i).setCellStyle(totalStyle);
            }
            
            totalRowObj.getCell(0).setCellValue("TOTAL");
            
            // Calculer la consommation totale
            int totalQuantity = history.stream().mapToInt(WaterIntake::getQuantityML).sum();
            
            totalRowObj.getCell(2).setCellValue(totalQuantity);
            totalRowObj.getCell(3).setCellValue(totalQuantity / 1000.0);
            
            workbook.write(out);
        }
        
        return out.toByteArray();
    }
    
    /**
     * Exporte les données d'hydratation au format PDF
     */
    public byte[] exportDataToPdf(UserDetails userDetails) throws Exception {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        List<WaterIntake> history = waterIntakeRepository.findByUserIdOrderByIntakeTimeDesc(user.getId());
        
        // Récupérer les paramètres d'hydratation de l'utilisateur
        WaterReminderSetting settings = getUserSettings(userDetails);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // Créer le document PDF
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        
        // Ajouter un pied de page
        HeaderFooter footer = new HeaderFooter(new Phrase("ZenLife - Rapport d'hydratation - Page ", FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, new Color(150, 150, 150))), true);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setBorderWidth(0);
        document.setFooter(footer);
        
        document.open();
        
        // Ajouter le titre
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(44, 62, 80));
        Paragraph title = new Paragraph("Rapport d'Hydratation ZenLife", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        // Ajouter la date
        Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 100, 100));
        Paragraph date = new Paragraph("Généré le: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")), dateFont);
        date.setAlignment(Element.ALIGN_CENTER);
        document.add(date);
        
        document.add(new Paragraph(" ")); // Espace
        
        // Ajouter des informations sur l'utilisateur
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(44, 62, 80));
        Paragraph userInfo = new Paragraph("Informations utilisateur", sectionFont);
        document.add(userInfo);
        
        document.add(new Paragraph(" ")); // Espace
        
        PdfPTable userTable = new PdfPTable(2);
        userTable.setWidthPercentage(100);
        
        // Style pour les cellules d'entête
        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(new Color(240, 240, 240));
        headerCell.setPadding(5);
        
        // Ligne: Nom d'utilisateur
        headerCell.setPhrase(new Phrase("Nom d'utilisateur"));
        userTable.addCell(headerCell);
        userTable.addCell(user.getUsername());
        
        // Ligne: Objectif quotidien
        headerCell.setPhrase(new Phrase("Objectif quotidien"));
        userTable.addCell(headerCell);
        userTable.addCell(settings.getDailyGoalML() + " ml (" + (settings.getDailyGoalML() / 1000.0) + " L)");
        
        document.add(userTable);
        
        document.add(new Paragraph(" ")); // Espace
        
        // Ajouter le résumé
        Paragraph summaryTitle = new Paragraph("Résumé", sectionFont);
        document.add(summaryTitle);
        
        document.add(new Paragraph(" ")); // Espace
        
        // Calculer les statistiques
        int totalQuantity = history.stream().mapToInt(WaterIntake::getQuantityML).sum();
        double dailyAverage = history.isEmpty() ? 0 : totalQuantity / (double) history.size();
        
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        
        // Ligne: Nombre d'entrées
        headerCell.setPhrase(new Phrase("Nombre d'entrées"));
        summaryTable.addCell(headerCell);
        summaryTable.addCell(String.valueOf(history.size()));
        
        // Ligne: Total consommé
        headerCell.setPhrase(new Phrase("Total consommé"));
        summaryTable.addCell(headerCell);
        summaryTable.addCell(totalQuantity + " ml (" + (totalQuantity / 1000.0) + " L)");
        
        // Ligne: Moyenne par entrée
        headerCell.setPhrase(new Phrase("Moyenne par entrée"));
        summaryTable.addCell(headerCell);
        summaryTable.addCell(String.format("%.2f ml (%.2f L)", dailyAverage, dailyAverage / 1000.0));
        
        document.add(summaryTable);
        
        document.add(new Paragraph(" ")); // Espace
        
        // Ajouter l'historique détaillé
        Paragraph historyTitle = new Paragraph("Historique détaillé", sectionFont);
        document.add(historyTitle);
        
        document.add(new Paragraph(" ")); // Espace
        
        if (history.isEmpty()) {
            document.add(new Paragraph("Aucune donnée d'hydratation enregistrée."));
        } else {
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            
            // Définir les largeurs relatives des colonnes
            float[] columnWidths = {2.5f, 1.5f, 1.5f, 1.5f};
            table.setWidths(columnWidths);
            
            // Style pour l'entête
            PdfPCell tableHeader = new PdfPCell();
            tableHeader.setBackgroundColor(new Color(41, 128, 185));
            tableHeader.setPadding(5);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            
            // Entêtes de colonnes
            tableHeader.setPhrase(new Phrase("Date", headerFont));
            table.addCell(tableHeader);
            
            tableHeader.setPhrase(new Phrase("Heure", headerFont));
            table.addCell(tableHeader);
            
            tableHeader.setPhrase(new Phrase("Quantité (ml)", headerFont));
            table.addCell(tableHeader);
            
            tableHeader.setPhrase(new Phrase("Quantité (L)", headerFont));
            table.addCell(tableHeader);
            
            // Données
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            
            // Style pour les lignes alternées
            PdfPCell evenRowCell = new PdfPCell();
            evenRowCell.setBackgroundColor(new Color(240, 240, 240));
            evenRowCell.setPadding(5);
            
            for (int i = 0; i < history.size(); i++) {
                WaterIntake intake = history.get(i);
                LocalDateTime dateTime = LocalDateTime.ofInstant(intake.getIntakeTime(), ZoneId.systemDefault());
                
                PdfPCell cell = (i % 2 == 0) ? evenRowCell : new PdfPCell();
                cell.setPadding(5);
                
                // Date
                cell.setPhrase(new Phrase(dateTime.format(dateFormatter)));
                table.addCell(cell);
                
                // Heure
                cell.setPhrase(new Phrase(dateTime.format(timeFormatter)));
                table.addCell(cell);
                
                // Quantité (ml)
                cell.setPhrase(new Phrase(String.valueOf(intake.getQuantityML())));
                table.addCell(cell);
                
                // Quantité (L)
                cell.setPhrase(new Phrase(String.format("%.2f", intake.getQuantityML() / 1000.0)));
                table.addCell(cell);
            }
            
            document.add(table);
        }
        
        document.close();
        
        return out.toByteArray();
    }
    
    /**
     * Réinitialise l'historique d'hydratation d'un utilisateur
     */
    @Transactional
    public void resetWaterIntakeHistory(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
        // Supprimer toutes les entrées d'hydratation de l'utilisateur
        waterIntakeRepository.deleteByUserId(user.getId());
        
        log.info("Historique d'hydratation réinitialisé pour l'utilisateur: {}", user.getUsername());
    }
}