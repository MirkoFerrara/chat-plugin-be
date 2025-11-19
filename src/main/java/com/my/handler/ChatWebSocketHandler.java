package com.my.handler;

import com.my.data.dto.ChatDTO;
import com.my.service.publisher.ChatPublisher;
import com.my.service.ChatService;
import com.my.service.redis.RedisEventBusPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

@Component("chatWebSocketHandler")  // ⭐ Nome bean specifico
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ChatPublisher chatPublisher;
    private final ChatService chatService;
    private final RedisEventBusPublisher eventBusPublisher;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // ⭐ UserId GIÀ validato dal wrapper (app principale)
        String userId = (String) session.getAttributes().get("userId");

        if (userId == null) {
            log.error("❌ UserId mancante negli attributi");
            return session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Non autenticato"));
        }

        String chatId = extractQueryParam(session, "chatId");
        log.info("🚀 Avvio session autenticata - ChatId: {}, UserId: {}", chatId, userId);

        // ✅ Input: Ricevi messaggi dal client
        Mono<Void> inbound = session.receive()
                .doOnNext(msg -> log.debug("📥 Messaggio ricevuto: {}", msg.getPayloadAsText()))
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(json -> processIncomingMessage(json, chatId, userId))
                .doOnError(e -> log.error("❌ Errore inbound: {}", e.getMessage()))
                .then();

        // ✅ Output: Invia messaggi al client
        Flux<WebSocketMessage> outbound = createOutboundFlux(session, chatId);

        return Mono.when(
                inbound.doFinally(sig -> log.info("inbound closed: {}", sig)),
                session.send(outbound).doFinally(sig -> log.info("outbound closed: {}", sig))
        ).doFinally(sig -> {
            log.info("🛑 WebSocket chiuso - Signal: {}", sig);
            chatPublisher.cleanupSink(chatId);
        });
    }

    private Mono<Void> processIncomingMessage(String json, String chatId, String userId) {
        return deserialize(json, chatId, userId)
                .flatMap(dto -> saveAndPublishWithErrorHandling(dto, chatId))
                .onErrorResume(e -> {
                    log.error("❌ Errore processing: {}", json, e);
                    return Mono.empty();
                });
    }

    private Mono<Void> saveAndPublishWithErrorHandling(ChatDTO dto, String chatId) {
        return chatService.saveAndPublish(dto)
                .then()
                .onErrorResume(e -> {
                    log.error("❌ Errore saveAndPublish: {}", dto, e);

                    ChatDTO errorDto = ChatDTO.builder()
                            .chatId(chatId)
                            .senderId("system")
                            .content("⚠️ Errore invio messaggio: " + e.getMessage())
                            .type("error")
                            .createdAt(Instant.now())
                            .build();

                    return eventBusPublisher.publish(errorDto).then();
                });
    }

    private Flux<WebSocketMessage> createOutboundFlux(WebSocketSession session, String chatId) {
        log.info("📡 Creazione outbound flux per chat: {}", chatId);

        Flux<ChatDTO> history = chatService.loadChatHistory(chatId)
                .doOnNext(msg -> log.info("📜 Storico: {}", msg.getContent()))
                .doOnComplete(() -> log.info("✅ Storico completato: {}", chatId));

        Flux<ChatDTO> realtime = chatPublisher.stream(chatId)
                .doOnNext(msg -> log.info("📤 Real-time: {}", msg.getContent()));

        Flux<ChatDTO> heartbeat = Flux.interval(Duration.ofSeconds(30))
                .map(tick -> {
                    log.debug("💓 Heartbeat per chat {}", chatId);
                    return ChatDTO.builder()
                            .chatId(chatId)
                            .senderId("system")
                            .content("heartbeat")
                            .type("heartbeat")
                            .createdAt(Instant.now())
                            .build();
                });

        return Flux.concat(history, Flux.merge(realtime, heartbeat))
                .map(chatService::serialize)
                .map(session::textMessage)
                .doOnSubscribe(s -> log.info("🔔 Subscriber attivo: {}", chatId));
    }

    private String extractQueryParam(WebSocketSession session, String param) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        if (query == null) return "";

        return Arrays.stream(query.split("&"))
                .filter(s -> s.startsWith(param + "="))
                .map(s -> s.split("=")[1])
                .findFirst()
                .orElse("");
    }

    private Mono<ChatDTO> deserialize(String json, String chatId, String userId) {
        return Mono.fromCallable(() -> chatService.deserialize(json, chatId, userId))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("❌ Deserializzazione fallita: {}", json, e);
                    return Mono.empty();
                });
    }
}