package com.example.javabigwork.report.dto;

import java.util.List;

public record LearningReportView(
        long learnedWords,
        long reviewWords,
        long favoriteWords,
        long essayCount,
        long readingCount,
        List<String> weakPoints,
        List<String> nextActions,
        long dueWords,
        LearningEvidence evidence
) {
}
