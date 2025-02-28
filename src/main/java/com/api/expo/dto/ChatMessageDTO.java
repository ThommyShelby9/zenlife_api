package com.api.expo.dto;

import lombok.Data;

@Data
public class ChatMessageDTO {
    private String receiverId;
    private String content;
}