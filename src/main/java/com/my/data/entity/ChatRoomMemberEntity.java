package com.my.data.entity;

 import com.my.data.UuidIdentifiable;
 import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Table("chat_room_members")
public class ChatRoomMemberEntity implements UuidIdentifiable {

    @Id
    private String id;

    @Column("chat_room_id")
    private String chatRoomId;

    @Column("user_id")
    private String userId;

    @Column("role_id")
    private String roleId;

    @Column("joined_at")
    private Instant joinedAt = Instant.now();
}
