package com.example.javabigwork.vocabulary.dto;

import java.util.List;

public record WordAiDetailView(
        String word,
        String partOfSpeech,
        List<Example> examples,
        List<Variant> variants,
        List<String> usageNotes,
        List<Collocation> collocations,
        String modelName,
        boolean cached
) {
    public record Example(String en, String cn) {
    }

    public record Variant(String form, String type, String meaningCn) {
    }

    public record Collocation(String phrase, String meaningCn, String exampleEn) {
    }
}
