package com.example.javabigwork.chat.dto;

import com.example.javabigwork.agent.AgentTraceStep;
import com.example.javabigwork.agent.dto.ChatAction;
import com.example.javabigwork.agent.dto.LearningCard;
import com.example.javabigwork.agent.dto.CoachFeedback;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record ChatResponse(
        Long conversationId,
        String reply,
        List<AgentTraceStep> trace,
        List<String> suggestions,
        LearningCard learningCard,
        List<ChatAction> actions,
        CoachFeedback coaching,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long messageId
) {
    public ChatResponse withMessageId(Long id) {
        return new ChatResponse(conversationId, reply, trace, suggestions, learningCard, actions, coaching, id);
    }
}
