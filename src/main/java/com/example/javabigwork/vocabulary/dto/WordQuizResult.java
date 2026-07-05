package com.example.javabigwork.vocabulary.dto;

public record WordQuizResult(
        Long wordId,
        boolean correct,
        String expectedMeaning,
        String exampleEn,
        String exampleCn,
        Integer familiarity,
        Integer wrongCount,
        String nextReviewHint
) {
}
