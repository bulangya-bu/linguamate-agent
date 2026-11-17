package com.example.javabigwork.report.dto;

import com.example.javabigwork.agent.dto.CoachFeedback;
import com.example.javabigwork.mission.dto.MissionView;
import java.time.LocalDateTime;
import java.util.List;

public record GrowthEvidenceView(
        List<WordEvidence> words,
        List<WritingEvidence> writing,
        List<MissionView> missions,
        List<ExpressionEvidence> expressions,
        List<ReadingEvidence> readings
) {
    public record ExpressionEvidence(Long id, Long conversationId, String title, String answer, String reply,
            CoachFeedback feedback, LocalDateTime createdAt, String mode, String scene) {}

    public record Attempt(Long id, String answer, boolean correct, String type, LocalDateTime createdAt,
            String spellingAnswer, String meaningAnswer, Boolean spellingCorrect, Boolean meaningCorrect) {}

    public record WordEvidence(Long wordId, String word, String meaning, List<Attempt> attempts) {}

    public record WritingEvidence(Long id, String title, Long previousId, String before, String after, String suggestion,
            Double previousScore, Double score, boolean comparable, String source, String model, LocalDateTime createdAt) {}

    public record ReadingEvidence(Long articleId, String title, String summary, String sourceName, String difficulty,
            int readProgress, int lookedUpWords, boolean finished, LocalDateTime finishedAt, LocalDateTime updatedAt) {}
}
