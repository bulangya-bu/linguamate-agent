package com.example.javabigwork.chat.dto;

import com.example.javabigwork.agent.AgentTraceStep;
import java.util.List;

public record ChatResponse(
        Long conversationId,
        String reply,
        List<AgentTraceStep> trace,
        List<String> suggestions
) {
}
