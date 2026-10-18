package com.example.javabigwork.agent.dto;

import java.time.LocalDateTime;
import java.util.List;

/** A bounded, current-user-only evidence snapshot for each assistant turn. */
public record LearningAssistantSnapshot(
        String date,
        LearningCard today,
        int trackedWords,
        long unfamiliarWords,
        long masteredWords,
        long mistakeWords,
        long dueWords,
        List<WordEvidence> priorityWords
) {
    public record WordEvidence(
            String word, String meaningCn, int familiarity, int wrongCount,
            int reviewCount, boolean due, LocalDateTime lastReviewAt, LocalDateTime nextReviewAt
    ) {}
}
