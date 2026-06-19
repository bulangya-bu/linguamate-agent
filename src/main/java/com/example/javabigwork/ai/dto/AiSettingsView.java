package com.example.javabigwork.ai.dto;

public record AiSettingsView(
        String provider,
        String baseUrl,
        String chatModel,
        boolean hasApiKey
) {
}
