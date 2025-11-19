package com.my.data.repository;

import com.my.data.entity.ChatMessageEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ChatMessageRepository extends ReactiveCrudRepository<ChatMessageEntity, String> {
    Flux<ChatMessageEntity> findByChatIdOrderByCreatedAtAsc(String chatId);
    @Query("SELECT * FROM chat_messages WHERE chat_id = :chatId ORDER BY created_at ASC, sequence ASC")
    Flux<ChatMessageEntity> findByChatIdOrderByCreatedAtAscAndSequenceAsc(String chatId);
}
