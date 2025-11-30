package com.my.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.data.entity.ChatRoomMemberEntity;
import com.my.data.repository.ChatRoomMemberRepository;
import com.my.service.publisher.EventBusPublisher;
import com.my.data.dto.ChatDTO;
import com.my.data.entity.ChatMessageEntity;
import com.my.data.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ObjectMapper mapper;
    private final ChatMessageRepository chatMessageRepository;
    private final EventBusPublisher eventBusPublisher;
    private final ChatRoomMemberRepository memberRepository;

    // --- Carica lo storico messaggi di una chat
    public Flux<ChatDTO> loadChatHistory(String chatId) {
        return chatMessageRepository.findByChatIdOrderByCreatedAtAscAndSequenceAsc(chatId)
                .map(this::convertToDto);
    }

    // --- Salva in DB e pubblica ai WebSocket locali + Redis
    public Mono<ChatDTO> saveAndPublish(ChatDTO dto) {
        ChatMessageEntity entity = convertToEntity(dto);
        return chatMessageRepository.save(entity)
                .map(this::convertToDto)
                .doOnNext(savedDto -> {
                    // ✅ Solo Redis, tutti ricevono uguale
                    eventBusPublisher.publish(savedDto).subscribe(
                            count -> log.debug("✅ Redis: {} subscriber", count),
                            error -> log.error("❌ Redis fallito: {}", error.getMessage())
                    );
                });
    }

    // --- Deserialize JSON in ChatDTO
    public ChatDTO deserialize(String json, String chatId, String userId)
            throws JsonProcessingException {
        ChatDTO chatMessage = mapper.readValue(json, ChatDTO.class);
        chatMessage.setChatId(chatId);
        chatMessage.setSenderId(userId);
        chatMessage.setCreatedAt(Instant.now());
        return chatMessage;
    }

    // --- Serialize ChatDTO in JSON
    public String serialize(ChatDTO msg) {
        try {
            return mapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Errore serializzazione JSON", e);
        }
    }

    public ChatDTO convertToDto(ChatMessageEntity entity) {
        return ChatDTO.builder()
                .id(entity.getId())
                .messageId(entity.getMessageId())
                .chatId(entity.getChatId())
                .senderId(entity.getSenderId())
                .content(entity.getContent())
                .type(entity.getType())
                .sequence(entity.getSequence())
                .createdAt(entity.getCreatedAt())
                .fileName(entity.getFileName())
                .fileUrl(entity.getFileUrl())
                .build();
    }

    private ChatMessageEntity convertToEntity(ChatDTO dto) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setChatId(dto.getChatId());
        entity.setMessageId(dto.getMessageId() != null ? dto.getMessageId() : UUID.randomUUID().toString());
        entity.setSenderId(dto.getSenderId());
        entity.setContent(dto.getContent());
        entity.setType(dto.getType());
        entity.setSequence(dto.getSequence() != null ? dto.getSequence() : 0);
        entity.setFileName(dto.getFileName());
        entity.setFileUrl(dto.getFileUrl());
        entity.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : Instant.now());
        return entity;
    }

    public boolean canAccessFile(String userId, String chatId) {
        return Boolean.TRUE.equals(memberRepository.findByChatRoomId(chatId)
                .map(ChatRoomMemberEntity::getUserId)
                .collectList()
                .map(list -> list.contains(userId))
                .block());
    }
}