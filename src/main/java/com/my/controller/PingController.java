package com.my.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public Mono<ResponseEntity<String>> ping(@RequestHeader(value="Authorization", required=false) String auth) {
        log.info("Received ping request - Authorization: {}", auth != null ? "[OK]" : "[MANCANTE]");
        return Mono.just(ResponseEntity.ok("pong"));
    }
}