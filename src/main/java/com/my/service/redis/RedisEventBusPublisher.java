package com.my.service.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.service.publisher.EventBusPublisher;
import com.my.data.dto.ChatDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEventBusPublisher implements EventBusPublisher {

    private static final String CHANNEL = "chat-broadcast";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper mapper;

    public Mono<Void> publish(ChatDTO msg) {
        return Mono.fromCallable(() -> mapper.writeValueAsString(msg))
                .flatMap(json -> redisTemplate.convertAndSend(CHANNEL, json))
                .doOnSuccess(id -> log.debug("📤 Messaggio pubblicato su Redis: {}", msg.getContent()))
                .onErrorResume(e -> {
                    log.error("❌ Errore pubblicazione Redis", e);
                    return Mono.empty();
                }).then();
    }
}
