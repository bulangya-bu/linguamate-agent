package com.example.javabigwork.dictionary.dto;

import java.util.List;

public record DictionaryApiEntry(
        String word,
        String phonetic,
        List<DictionaryApiPhonetic> phonetics,
        List<DictionaryApiMeaning> meanings,
        List<String> sourceUrls
) {
    public record DictionaryApiPhonetic(
            String text,
            String audio
    ) {
    }

    public record DictionaryApiMeaning(
            String partOfSpeech,
            List<DictionaryApiDefinition> definitions
    ) {
    }

    public record DictionaryApiDefinition(
            String definition,
            String example
    ) {
    }
}
