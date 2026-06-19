package com.example.javabigwork.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "linguamate.ai")
public record AiProperties(
        String provider,
        String baseUrl,
        String apiKey,
        String chatModel,
        String embeddingModel
) {
}
