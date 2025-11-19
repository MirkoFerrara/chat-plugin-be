package com.my.service.publisher;

import com.my.data.dto.ChatDTO;
import reactor.core.publisher.Mono;

public interface EventBusPublisher {
    Mono<Void> publish(ChatDTO message);
}
