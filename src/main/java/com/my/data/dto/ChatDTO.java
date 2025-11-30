package com.my.data.dto;

import lombok.*;

import java.time.Instant;

@Data
@Builder
public class ChatDTO {
    private Long id;
    private String messageId;
    private String chatId;
    private String senderId;
    private String content;
    private String type;
    private Integer sequence;
    private Instant createdAt;
    private String fileUrl;
    private String fileName;
}
