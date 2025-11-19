package com.my.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class UploadFileInChatController {

    @PostMapping(value = "/uploadFiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<List<Map<String, String>>>> uploadFiles(
            @RequestParam("chatId") String chatId,
            @RequestPart("files") Flux<FilePart> files
    ) {
        Path uploadDir = Paths.get("uploads/");
        if (!Files.exists(uploadDir)) {
            try { Files.createDirectories(uploadDir); }
            catch (IOException e) { return Mono.error(e); }
        }

        return files
                .flatMap(file -> {
                    String filename = UUID.randomUUID() + "_" + file.filename().replaceAll("\\s+", "_");
                    Path path = uploadDir.resolve(filename);
                    return file.transferTo(path)
                            .thenReturn(Map.of("fileUrl", "/uploads/" + filename));
                })
                .collectList()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(
                            ResponseEntity.status(500)
                                    .body(List.of(Map.of("error", e.getMessage())))
                    );
                });
    }

    @GetMapping("/file/{filename}")
    @PreAuthorize("@chatService.canAccessFile(authentication.principal, #chatId)")
    public Mono<ResponseEntity<Resource>> getFile(
            @PathVariable String filename,
            @RequestParam String chatId
    ) {
        Path filePath = Paths.get("uploads").resolve(filename);
        Resource resource = new FileSystemResource(filePath.toFile());

        MediaType mediaType = MediaTypeFactory.getMediaType(filePath.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        boolean isDisplayable = mediaType.getType().equalsIgnoreCase("image")
                || mediaType.equals(MediaType.APPLICATION_PDF);

        String disposition = isDisplayable ? "inline" : "attachment";

        return Mono.just(
                ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + filename + "\"")
                        .contentType(mediaType)
                        .body(resource)
        );
    }
}
