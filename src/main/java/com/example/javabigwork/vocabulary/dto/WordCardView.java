package com.example.javabigwork.vocabulary.dto;

import java.util.List;

public record WordCardView(
        Long id,
        String word,
        String phoneticUs,
        String phoneticUk,
        String meaningCn,
        String partOfSpeech,
        String exampleEn,
        String exampleCn,
        String levelTag,
        Integer familiarity,
        Integer wrongCount,
        Boolean favorite,
        List<WordDefinition> definitions,
        String sourceSentence,
        String sourceTitle,
        String sourceWord,
        Long sourceArticleId
) {
    public WordCardView(Long id, String word, String phoneticUs, String phoneticUk, String meaningCn,
                        String partOfSpeech, String exampleEn, String exampleCn, String levelTag,
                        Integer familiarity, Integer wrongCount, Boolean favorite, List<WordDefinition> definitions) {
        this(id, word, phoneticUs, phoneticUk, meaningCn, partOfSpeech, exampleEn, exampleCn, levelTag,
                familiarity, wrongCount, favorite, definitions, null, null, null, null);
    }
}
