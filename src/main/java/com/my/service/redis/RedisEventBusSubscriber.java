package com.my.service.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.data.dto.ChatDTO;
import com.my.service.publisher.ChatPublisher;
import com.my.service.subscriber.EventBusSubscriber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisEventBusSubscriber implements EventBusSubscriber {

    private static final String CHANNEL = "chat-broadcast";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ChatPublisher chatPublisher;
    private final ObjectMapper mapper;

    @PostConstruct
    public void start() {
        redisTemplate.listenToChannel(CHANNEL)
                .map(ReactiveSubscription.Message::getMessage)
                .flatMap(this::deserialize)
                .doOnNext(chatPublisher::publish)  // Invia solo ai WebSocket locali
                .doOnNext(msg -> log.debug("📥 Ricevuto via Redis: {}", msg.getContent()))
                .onErrorContinue((err, obj) -> log.error("❌ Redis listener error", err))
                .subscribe();
    }

    private Mono<ChatDTO> deserialize(String json) {
        return Mono.fromCallable(() -> mapper.readValue(json, ChatDTO.class));
    }

    @Override
    public void stop() {
        log.info("🛑 Redis Pub/Sub subscriber stopped");
    }
}
