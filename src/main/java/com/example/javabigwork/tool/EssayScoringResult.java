package com.example.javabigwork.tool;

import com.example.javabigwork.writing.dto.SentenceFeedbackView;
import java.util.List;

public record EssayScoringResult(
        double totalScore,
        double grammarScore,
        double vocabularyScore,
        double structureScore,
        double logicScore,
        String summary,
        String polishedText,
        List<SentenceFeedbackView> feedback
) {
}
