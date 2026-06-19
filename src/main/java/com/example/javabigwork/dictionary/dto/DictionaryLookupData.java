package com.example.javabigwork.dictionary.dto;

public record DictionaryLookupData(
        String word,
        String phoneticUs,
        String phoneticUk,
        String partOfSpeech,
        String definitionEn,
        String exampleEn,
        String sourceUrl
) {
}
