package com.my.data.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatRoomResponseDTO {
    private String id;
    private String name;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
}
