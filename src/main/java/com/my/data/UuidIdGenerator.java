package com.my.data;

import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class UuidIdGenerator implements BeforeConvertCallback<UuidIdentifiable> {

    @Override
    public Mono<UuidIdentifiable> onBeforeConvert(UuidIdentifiable entity, SqlIdentifier table) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
        }
        return Mono.just(entity);
    }
}