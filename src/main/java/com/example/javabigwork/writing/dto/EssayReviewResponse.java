package com.example.javabigwork.writing.dto;

import com.example.javabigwork.agent.AgentTraceStep;
import java.util.List;

public record EssayReviewResponse(
        Long essayId,
        double totalScore,
        double grammarScore,
        double vocabularyScore,
        double structureScore,
        double logicScore,
        String summary,
        String polishedText,
        List<SentenceFeedbackView> feedback,
        List<AgentTraceStep> trace,
        String reviewSource,
        String modelName
) {
}
