package com.example.javabigwork.chat.dto;

import com.example.javabigwork.agent.AgentTraceStep;
import com.example.javabigwork.agent.dto.ChatAction;
import com.example.javabigwork.agent.dto.LearningCard;
import com.example.javabigwork.agent.dto.CoachFeedback;
import java.util.List;

public record ChatResponse(
        Long conversationId,
        String reply,
        List<AgentTraceStep> trace,
        List<String> suggestions,
        LearningCard learningCard,
        List<ChatAction> actions,
        CoachFeedback coaching
) {
}
