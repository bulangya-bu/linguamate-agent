package com.example.javabigwork.vocabulary.service;

import com.example.javabigwork.vocabulary.dto.WordDefinition;
import com.example.javabigwork.vocabulary.entity.Word;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** Retains every local-bank translation without changing the quiz's primary answer. */
@Service
public class WordDefinitionService {
    private final Map<String, List<WordDefinition>> definitions = new ConcurrentHashMap<>();

    public void register(String word, List<WordDefinition> entries) {
        if (word == null || word.isBlank()) return;
        definitions.compute(normalizeWord(word), (key, existing) ->
                merge(existing == null ? List.of() : existing, entries));
    }

    public List<WordDefinition> definitionsFor(Word word) {
        if (word == null) return List.of();
        return merge(
                List.of(new WordDefinition(word.partOfSpeech, word.meaningCn)),
                definitions.getOrDefault(normalizeWord(word.word), List.of())
        );
    }

    private List<WordDefinition> merge(List<WordDefinition> first, List<WordDefinition> second) {
        Map<String, WordDefinition> result = new LinkedHashMap<>();
        for (List<WordDefinition> entries : List.of(first, second)) {
            for (WordDefinition entry : entries) {
                if (entry == null || entry.meaningCn() == null || entry.meaningCn().isBlank()) continue;
                String pos = entry.partOfSpeech() == null ? "" : entry.partOfSpeech().trim();
                String meaning = entry.meaningCn().trim();
                String key = pos.toLowerCase(Locale.ROOT).replace(".", "")
                        + ":" + meaning.replaceAll("[\\s,，;；]+", "");
                result.putIfAbsent(key, new WordDefinition(pos, meaning));
            }
        }
        return List.copyOf(result.values());
    }

    private String normalizeWord(String word) {
        return word == null ? "" : word.trim().toLowerCase(Locale.ROOT);
    }
}
