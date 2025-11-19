package com.my.data.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatRoomRequestDTO {
    private String chatId;
    private String name;
    private List<String> participantIds ;
}