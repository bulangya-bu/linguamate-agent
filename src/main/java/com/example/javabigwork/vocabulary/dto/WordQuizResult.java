package com.example.javabigwork.vocabulary.dto;

public record WordQuizResult(
        Long wordId,
        boolean correct,
        String expectedWord,
        String expectedMeaning,
        String exampleEn,
        String exampleCn,
        Integer familiarity,
        Integer wrongCount,
        String nextReviewHint,
        Boolean spellingCorrect,
        Boolean meaningCorrect
) {
    public WordQuizResult(
            Long wordId,
            boolean correct,
            String expectedMeaning,
            String exampleEn,
            String exampleCn,
            Integer familiarity,
            Integer wrongCount,
            String nextReviewHint
    ) {
        this(
                wordId,
                correct,
                null,
                expectedMeaning,
                exampleEn,
                exampleCn,
                familiarity,
                wrongCount,
                nextReviewHint,
                null,
                null
        );
    }
}
