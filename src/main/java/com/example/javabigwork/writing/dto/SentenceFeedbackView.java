package com.example.javabigwork.writing.dto;

public record SentenceFeedbackView(
        String category,
        String originalSentence,
        String suggestion,
        String correctedSentence
) {
}
