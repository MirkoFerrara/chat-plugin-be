package com.my.service;

import com.my.data.dto.ChatRoomRequestDTO;
import com.my.data.dto.ChatRoomResponseDTO;
import com.my.data.entity.ChatRoomEntity;
import com.my.data.entity.ChatRoomMemberEntity;
import com.my.data.repository.ChatRoomMemberRepository;
import com.my.data.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository memberRepository;

    public Mono<ChatRoomResponseDTO> getChatRoom(ChatRoomRequestDTO requestDTO,String creatorId) {

        String chatId = requestDTO.getChatId();

        if(chatId != null && !chatId.isEmpty()){
            return chatRoomRepository.findById(chatId)
                    .flatMap(this::convertToRest);
        } else {

            List<String> participantIds = requestDTO.getParticipantIds();

            if (participantIds.size() == 2 && (requestDTO.getName() == null || requestDTO.getName().isEmpty())) {
                log.info("🔍 Ricerca o creazione chat 1-a-1 tra: {}", participantIds);
                return findOrCreateDirectChat(participantIds.get(0), participantIds.get(1));
            }

            log.info("👥 Creazione chat di gruppo: {}", requestDTO.getName());
            return createGroupChat(requestDTO, creatorId);
        }
    }

    private Mono<ChatRoomResponseDTO> findOrCreateDirectChat(String userId1, String userId2) {
        // Trova tutte le chat del primo utente
        return memberRepository.findByUserId(userId1)
                .map(ChatRoomMemberEntity::getChatRoomId)
                .collectList()
                .flatMap(chatRoomIds -> {
                    if (chatRoomIds.isEmpty()) {
                        return createDirectChat(userId1, userId2);
                    }

                    // Per ogni chat, verifica se contiene entrambi gli utenti
                    return Flux.fromIterable(chatRoomIds)
                            .flatMap(chatRoomId ->
                                    memberRepository.findByChatRoomId(chatRoomId)
                                            .map(ChatRoomMemberEntity::getUserId)
                                            .collectList()
                                            .flatMap(members -> {
                                                // Se questa chat ha esattamente questi 2 membri
                                                if (members.size() == 2 &&
                                                        members.contains(userId1) &&
                                                        members.contains(userId2)) {

                                                    log.info("✅ Trovata chat 1-a-1 esistente: {}", chatRoomId);
                                                    return chatRoomRepository.findById(chatRoomId)
                                                            .flatMap(this::convertToRest);
                                                }
                                                return Mono.empty();
                                            })
                            )
                            .next() // Prendi la prima trovata
                            .switchIfEmpty(createDirectChat(userId1, userId2)); // O crea nuova
                });
    }

    private Mono<ChatRoomResponseDTO> createDirectChat(String userId1, String userId2) {
        log.info("🆕 Creazione nuova chat 1-a-1");

        ChatRoomEntity room = new ChatRoomEntity();
        room.setName("");
        room.setCreatedBy(userId1);
        room.setIsGroup(false);

        return chatRoomRepository.save(room)
                .flatMap(saved -> {
                    // Aggiungi entrambi i membri
                    ChatRoomMemberEntity member1 = new ChatRoomMemberEntity();
                    member1.setChatRoomId(saved.getId());
                    member1.setUserId(userId1);
                    member1.setRoleId("member");

                    ChatRoomMemberEntity member2 = new ChatRoomMemberEntity();
                    member2.setChatRoomId(saved.getId());
                    member2.setUserId(userId2);
                    member2.setRoleId("member");

                    return memberRepository.saveAll(List.of(member1, member2))
                            .then(Mono.just(saved));
                })
                .flatMap(this::convertToRest)
                .doOnSuccess(c -> log.info("✅ Chat 1-a-1 creata: {}", c.getId()));
    }

    public Mono<ChatRoomResponseDTO> createGroupChat(ChatRoomRequestDTO request, String creatorId) {
        ChatRoomEntity room = new ChatRoomEntity();
        room.setName(request.getName()==null?"Gruppo-"+ UUID.randomUUID() :request.getName());
        room.setCreatedBy(creatorId);
        room.setIsGroup(true);

        return chatRoomRepository.save(room)
                .flatMap(saved -> {
                    List<ChatRoomMemberEntity> members = getChatRoomParticipants(request, saved.getId(), creatorId);
                    return memberRepository.saveAll(members).then(Mono.just(saved));
                })
                .flatMap(this::convertToRest)
                .doOnSuccess(c -> log.info("✅ Gruppo creato: {}", c.getName()));
    }

    private List<ChatRoomMemberEntity> getChatRoomParticipants(
            ChatRoomRequestDTO request,
            String chatRoomId,
            String creatorId
    ) {
        return request.getParticipantIds()
                .stream()
                .map(uid -> {
                    ChatRoomMemberEntity member = new ChatRoomMemberEntity();
                    member.setChatRoomId(chatRoomId);
                    member.setUserId(uid);
                    member.setRoleId(uid.equals(creatorId) ? "owner" : "member");
                    return member;
                })
                .toList();
    }

    private List<ChatRoomMemberEntity> getChatRoomPartecipants(ChatRoomRequestDTO request,String chatRoomId,String creatorId) {
        return request.getParticipantIds()
                .stream()
                .map(uid -> {
                    ChatRoomMemberEntity chatRoomMember = new ChatRoomMemberEntity();
                    chatRoomMember.setChatRoomId(chatRoomId);
                    chatRoomMember.setUserId(uid);
                    chatRoomMember.setRoleId(uid.equals(creatorId) ? "owner" : "member");
                    return chatRoomMember;
                }).toList();
    }

    private Mono<ChatRoomResponseDTO> convertToRest(ChatRoomEntity chatRoomEntity){
        return Mono.just(ChatRoomResponseDTO.builder()
                .id(chatRoomEntity.getId())
                .name(chatRoomEntity.getName())
                .createdBy(chatRoomEntity.getCreatedBy())
                .createdAt(String.valueOf(chatRoomEntity.getCreatedAt()))
                .updatedAt(chatRoomEntity.getUpdatedAt() == null ? null : chatRoomEntity.getUpdatedAt().toString())
                .build());
    }

    public Mono<Void> addUserToGroupChat(String chatId, String userToAddId, String role) {
        ChatRoomMemberEntity member = new ChatRoomMemberEntity();
        member.setChatRoomId(chatId);
        member.setUserId(userToAddId);
        member.setRoleId(role);
        return memberRepository.save(member)
                .then()
                .doOnSuccess(v -> log.info("👥 Utente {} aggiunto alla chat {}", userToAddId, chatId));
    }
}
