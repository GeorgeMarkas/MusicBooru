package com.example.musicbooru.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ErrorResponse(
        HttpStatus status,
        String message,

        @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
        Instant timestamp,

        String path
) {
    public ErrorResponse(HttpStatus status, String message, String path) {
        this(status, message, Instant.now(), path);
    }
}
