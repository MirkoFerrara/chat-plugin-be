package com.my.controller;

import com.my.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import com.my.data.dto.ChatRoomRequestDTO;
import com.my.data.dto.ChatRoomResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class GroupChatController {

    private final ChatRoomService chatRoomService;

    @PostMapping("/getChatRoom")
    public Mono<ResponseEntity<ChatRoomResponseDTO>> getChatRoom(@RequestBody Mono<ChatRoomRequestDTO> request, ServerHttpRequest httpRequest) {

        String creatorId = httpRequest.getHeaders().getFirst("UserId");

        log.info("👤 CreatorId dall'header: {}", creatorId);

        if (creatorId == null || creatorId.isEmpty()) {
            log.error("❌ UserId header mancante!");
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return request.flatMap(req->chatRoomService.getChatRoom(req,creatorId))
                .map(dto -> {
                    log.info("✅ Chat room: {}", dto.getId());
                    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
                }).doOnError(e -> log.error("❌ Errore getChatRoom", e));
    }

    @PostMapping("/{chatId}/add/{userId}")
    public Mono<ResponseEntity<Void>> addUser(@PathVariable String chatId, @PathVariable String userId, @PathVariable String role) {
        return chatRoomService.addUserToGroupChat(chatId, userId, role)
                .thenReturn(ResponseEntity.ok().build());
    }
 }
