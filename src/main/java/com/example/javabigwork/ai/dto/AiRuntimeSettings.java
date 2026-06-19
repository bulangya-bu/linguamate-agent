package com.example.javabigwork.ai.dto;

public record AiRuntimeSettings(
        String provider,
        String baseUrl,
        String apiKey,
        String chatModel
) {
}
