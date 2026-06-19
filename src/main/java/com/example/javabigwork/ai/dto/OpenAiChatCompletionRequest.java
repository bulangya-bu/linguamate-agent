package com.example.javabigwork.ai.dto;

import java.util.List;

public record OpenAiChatCompletionRequest(
        String model,
        List<OpenAiChatMessage> messages,
        boolean stream
) {
}
