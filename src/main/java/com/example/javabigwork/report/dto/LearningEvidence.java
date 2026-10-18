package com.example.javabigwork.report.dto;

public record LearningEvidence(long recentAttempts, Long recentAccuracy, long previousAttempts,
                               Long previousAccuracy, long recurringWords, long revisedEssays) {
}
