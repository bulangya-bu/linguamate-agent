package com.example.javabigwork.dictionary.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CommonWordPoolService {
    private static final String WORD_POOL_URL =
            "https://raw.githubusercontent.com/first20hours/google-10000-english/master/google-10000-english-no-swears.txt";
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "that", "this", "you", "your", "are", "was", "were", "have", "has", "had",
            "not", "but", "can", "will", "would", "could", "should", "from", "their", "there", "they", "them", "what",
            "when", "where", "which", "who", "how", "why", "into", "out", "over", "under", "after", "before",
            "because", "while", "also", "more", "most", "some", "such", "only", "very", "much", "many", "each",
            "other", "same", "than", "then", "too", "about", "yourself", "ours", "ourselves", "theirs", "it", "its",
            "a", "an", "to", "of", "in", "on", "at", "by", "be", "as", "or", "if", "so", "do", "does", "did"
    );

    private final RestClient restClient;
    private volatile List<String> cachedWords;

    public CommonWordPoolService() {
        this.restClient = RestClient.builder().build();
        this.cachedWords = List.of();
    }

    public List<String> sample(int limit) {
        List<String> pool = loadPool();
        if (pool.isEmpty()) {
            return List.of();
        }
        int safeLimit = Math.max(1, limit);
        List<String> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        return shuffled.stream().limit(Math.min(shuffled.size(), safeLimit)).toList();
    }

    public List<String> loadPool() {
        List<String> snapshot = cachedWords;
        if (!snapshot.isEmpty()) {
            return snapshot;
        }
        synchronized (this) {
            if (!cachedWords.isEmpty()) {
                return cachedWords;
            }
            try {
                String text = restClient.get()
                        .uri(WORD_POOL_URL)
                        .retrieve()
                        .body(String.class);
                if (text == null || text.isBlank()) {
                    cachedWords = List.of();
                    return cachedWords;
                }
                LinkedHashSet<String> words = new LinkedHashSet<>();
                for (String line : text.split("\\R")) {
                    String normalized = normalize(line);
                    if (isAllowed(normalized)) {
                        words.add(normalized);
                        if (words.size() >= 4000) {
                            break;
                        }
                    }
                }
                cachedWords = List.copyOf(words);
                return cachedWords;
            } catch (Exception exception) {
                cachedWords = List.of();
                return cachedWords;
            }
        }
    }

    private String normalize(String word) {
        return word == null ? "" : word.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isAllowed(String word) {
        return word != null
                && word.length() >= 4
                && word.length() <= 20
                && word.matches("[a-z][a-z'-]*")
                && !STOP_WORDS.contains(word);
    }
}
