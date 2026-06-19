package com.example.javabigwork.dictionary.dto;

public record DictionaryTranslationRequest(
        String word,
        String partOfSpeech,
        String definitionEn,
        String exampleEn
) {
}
