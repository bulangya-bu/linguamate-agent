package com.example.javabigwork.ai.dto;

public record AiSettingsRequest(
        String baseUrl,
        String apiKey,
        String chatModel
) {
}
