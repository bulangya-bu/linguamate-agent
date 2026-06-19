package com.example.javabigwork.writing.dto;

import java.util.List;

public record EssayReviewAiPayload(
        Double totalScore,
        Double grammarScore,
        Double vocabularyScore,
        Double structureScore,
        Double logicScore,
        String summary,
        String polishedText,
        List<SentenceFeedbackView> feedback
) {
}
