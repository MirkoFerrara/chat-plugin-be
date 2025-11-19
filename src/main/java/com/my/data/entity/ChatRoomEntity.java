package com.my.data.entity;

 import com.my.data.UuidIdentifiable;
 import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Table("chat_room")
public class ChatRoomEntity implements UuidIdentifiable {

    @Id
    private String id;

    @Column("name")
    private String name;

    @Column("is_group")
    private Boolean isGroup = false;

    @Column("created_by")
    private String createdBy;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    @Column("updated_at")
    private Instant updatedAt = Instant.now();
}

