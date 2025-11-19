package com.my.service.publisher;

import com.my.data.dto.ChatDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatPublisher {

    private final Map<String, Sinks.Many<ChatDTO>> chatSinks = new ConcurrentHashMap<>();

    /**
     * Pubblica un messaggio verso tutti i WebSocket locali della chat.
     */
    public void publish(ChatDTO message) {
        chatSinks.computeIfPresent(message.getChatId(), (id, sink) -> {
            sink.tryEmitNext(message);
            return sink;
        });
    }

    /**
     * Stream di messaggi di una chat specifica.
     */
    public Flux<ChatDTO> stream(String chatId) {
        Sinks.Many<ChatDTO> sink = chatSinks.computeIfAbsent(
                chatId, id -> Sinks.many().multicast().onBackpressureBuffer()
        );
        return sink.asFlux();
    }

    /**
     * Rimuove i sink di chat inattive per liberare memoria.
     */
    public void cleanupSink(String chatId) {
        chatSinks.remove(chatId);
    }
}
