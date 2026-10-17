package com.example.javabigwork.agent;

import com.example.javabigwork.agent.dto.ChatAction;
import com.example.javabigwork.agent.dto.LearningCard;
import java.util.List;

public record AgentResult(
        String reply,
        List<AgentTraceStep> trace,
        List<String> suggestions,
        LearningCard learningCard,
        List<ChatAction> actions) {
}
