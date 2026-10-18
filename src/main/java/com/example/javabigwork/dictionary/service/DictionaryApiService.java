package com.example.javabigwork.dictionary.service;

import com.example.javabigwork.dictionary.dto.DictionaryApiEntry;
import com.example.javabigwork.dictionary.dto.DictionaryLookupData;
import java.util.List;
import java.util.Optional;
import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class DictionaryApiService {
    private static final String BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries/en";

    private final RestClient restClient;

    public DictionaryApiService() {
        SimpleClientHttpRequestFactory transport = new SimpleClientHttpRequestFactory();
        transport.setConnectTimeout(Duration.ofSeconds(2));
        transport.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().baseUrl(BASE_URL).requestFactory(transport).build();
    }

    public Optional<DictionaryLookupData> lookup(String normalizedWord) {
        if (!StringUtils.hasText(normalizedWord)) {
            return Optional.empty();
        }
        try {
            DictionaryApiEntry[] entries = restClient.get()
                    .uri("/{word}", normalizedWord)
                    .retrieve()
                    .body(DictionaryApiEntry[].class);
            if (entries == null || entries.length == 0) {
                return Optional.empty();
            }
            return Optional.of(toLookupData(entries[0]));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private DictionaryLookupData toLookupData(DictionaryApiEntry entry) {
        String partOfSpeech = "";
        String definition = "";
        String example = "";
        if (entry.meanings() != null) {
            for (DictionaryApiEntry.DictionaryApiMeaning meaning : entry.meanings()) {
                if (meaning == null || !StringUtils.hasText(meaning.partOfSpeech())) {
                    continue;
                }
                partOfSpeech = meaning.partOfSpeech();
                if (meaning.definitions() != null && !meaning.definitions().isEmpty()) {
                    DictionaryApiEntry.DictionaryApiDefinition first = meaning.definitions().get(0);
                    if (first != null) {
                        definition = safeText(first.definition());
                        example = safeText(first.example());
                    }
                }
                if (StringUtils.hasText(definition)) {
                    break;
                }
            }
        }
        return new DictionaryLookupData(
                safeText(entry.word()),
                pickPhonetic(entry.phonetic(), entry.phonetics(), true),
                pickPhonetic(entry.phonetic(), entry.phonetics(), false),
                safeText(partOfSpeech),
                safeText(definition),
                safeText(example),
                entry.sourceUrls() == null || entry.sourceUrls().isEmpty() ? "" : safeText(entry.sourceUrls().get(0))
        );
    }

    private String pickPhonetic(
            String fallback,
            List<DictionaryApiEntry.DictionaryApiPhonetic> phonetics,
            boolean preferWithAudio
    ) {
        if (phonetics != null && !phonetics.isEmpty()) {
            for (DictionaryApiEntry.DictionaryApiPhonetic phonetic : phonetics) {
                if (phonetic == null || !StringUtils.hasText(phonetic.text())) {
                    continue;
                }
                boolean hasAudio = StringUtils.hasText(phonetic.audio());
                if (preferWithAudio == hasAudio) {
                    return phonetic.text().trim();
                }
            }
            for (DictionaryApiEntry.DictionaryApiPhonetic phonetic : phonetics) {
                if (phonetic != null && StringUtils.hasText(phonetic.text())) {
                    return phonetic.text().trim();
                }
            }
        }
        return safeText(fallback);
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }
}
