package com.my.data.repository;

import com.my.data.entity.ChatRoomEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomRepository extends ReactiveCrudRepository<ChatRoomEntity, String> {

}

