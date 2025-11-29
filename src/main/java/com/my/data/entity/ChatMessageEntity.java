package com.my.data.entity;

import com.my.data.UuidIdentifiable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Table("chat_messages")
public class ChatMessageEntity {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column("message_id")
    private String messageId;

    @Column("chat_id")
    private String chatId;

    @Column("sender_id")
    private String senderId;

    @Column("content")
    private String content;

    @Column("type")
    private String type;

    @Column("sequence")
    private Integer sequence;

    @Column("created_at")
    private Instant createdAt;

    @Column("file_url")
    private String fileUrl;

    @Column("file_name")
    private String fileName;
}