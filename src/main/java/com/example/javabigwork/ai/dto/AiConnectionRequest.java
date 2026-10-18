package com.example.javabigwork.ai.dto;

public record AiConnectionRequest(
        String baseUrl,
        String apiKey,
        String chatModel
) {
    public AiConnectionRequest(String baseUrl, String apiKey) { this(baseUrl, apiKey, null); }
}
