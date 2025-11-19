package com.my.service.kafka_NOT_USED;


import com.my.data.dto.ChatDTO;
import com.my.service.publisher.ChatPublisher;
import com.my.service.subscriber.EventBusSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "messaging.backend", havingValue = "kafka")
public class KafkaEventBusSubscriber implements EventBusSubscriber {

    private final ChatPublisher chatPublisher;

    @Override
    public void start() {
        log.info("🚀 Kafka subscriber started (via @KafkaListener)");
    }

    @KafkaListener(topics = "chat-messages", groupId = "chat-realtime-${random.uuid}")
    public void consume(ChatDTO message) {
        log.debug("📥 Kafka: ricevuto msg {}", message.getId());
        chatPublisher.publish(message);
    }

    @Override
    public void stop() {
        log.info("🛑 Kafka subscriber stopped");
    }
}