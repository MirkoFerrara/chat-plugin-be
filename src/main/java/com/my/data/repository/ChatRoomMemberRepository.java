package com.my.data.repository;

import com.my.data.entity.ChatRoomMemberEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ChatRoomMemberRepository extends ReactiveCrudRepository<ChatRoomMemberEntity, String> {
    Flux<ChatRoomMemberEntity> findByChatRoomId(String chatRoomId);
    Flux<ChatRoomMemberEntity> findByUserId(String userId);
}
