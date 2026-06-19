package com.example.javabigwork.ai.dto;

import java.util.List;

public record OpenAiChatCompletionResponse(
        List<Choice> choices
) {
    public record Choice(OpenAiChatMessage message) {
    }
}
