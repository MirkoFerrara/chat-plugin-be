package com.my.service.kafka_NOT_USED;


import com.my.service.publisher.EventBusPublisher;
import com.my.data.dto.ChatDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.backend", havingValue = "kafka")
public class KafkaEventBusPublisher implements EventBusPublisher {

    private static final String TOPIC = "chat-messages";
    private final KafkaTemplate<String, ChatDTO> kafkaTemplate;

    @Override
    public Mono<Void> publish(ChatDTO message) {
        return Mono.fromFuture(kafkaTemplate.send(TOPIC, message.getChatId(), message))  // ✅ Basta così!
                .doOnSuccess(result -> log.debug("✅ Kafka: pubblicato msg {} su partition {}",
                        message.getId(),
                        result.getRecordMetadata().partition()))
                .doOnError(e -> log.error("❌ Kafka pubblicazione fallita per msg {}: {}",
                        message.getId(), e.getMessage()))
                .then();
    }
}