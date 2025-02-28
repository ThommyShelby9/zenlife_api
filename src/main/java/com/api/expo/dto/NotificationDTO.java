package com.api.expo.dto;
import lombok.Data;


@Data
public class NotificationDTO {
    private String type;
    private String content;
    private String link;
}