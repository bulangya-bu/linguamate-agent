package com.example.javabigwork.dictionary.service;

import com.example.javabigwork.ai.dto.AiRuntimeSettings;
import com.example.javabigwork.ai.dto.OpenAiChatMessage;
import com.example.javabigwork.ai.service.AiSettingsService;
import com.example.javabigwork.ai.service.OpenAiCompatibleClient;
import com.example.javabigwork.dictionary.dto.DictionaryTranslationItem;
import com.example.javabigwork.dictionary.dto.DictionaryTranslationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DictionaryTranslationService {
    private final AiSettingsService settingsService;
    private final OpenAiCompatibleClient client;
    private final ObjectMapper objectMapper;

    public DictionaryTranslationService(
            AiSettingsService settingsService,
            OpenAiCompatibleClient client,
            ObjectMapper objectMapper
    ) {
        this.settingsService = settingsService;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public Map<String, DictionaryTranslationItem> translateBatch(List<DictionaryTranslationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Map.of();
        }
        AiRuntimeSettings settings = settingsService.resolveCurrent();
        if (!StringUtils.hasText(settings.baseUrl())
                || !StringUtils.hasText(settings.apiKey())
                || !StringUtils.hasText(settings.chatModel())) {
            return fallbackBatch(requests);
        }
        try {
            String raw = client.chat(settings, systemPrompt(), List.of(new OpenAiChatMessage("user", userPrompt(requests))));
            List<DictionaryTranslationItem> items = parseItems(raw);
            Map<String, DictionaryTranslationItem> translated = new LinkedHashMap<>();
            for (DictionaryTranslationItem item : items) {
                if (item != null && StringUtils.hasText(item.word())) {
                    translated.put(item.word().toLowerCase(), normalizeItem(item));
                }
            }
            if (translated.isEmpty()) {
                return fallbackBatch(requests);
            }
            return translated;
        } catch (Exception exception) {
            return fallbackBatch(requests);
        }
    }

    public DictionaryTranslationItem translateSingle(DictionaryTranslationRequest request) {
        return translateBatch(List.of(request)).getOrDefault(
                normalizeWord(request.word()),
                fallbackItem(request)
        );
    }

    private String systemPrompt() {
        return """
                You are a bilingual English-Chinese dictionary assistant.
                Translate the given dictionary entries into concise Chinese.
                Return only a JSON array with the same order as the input array.
                Each item must contain:
                - word
                - meaningCn: a short Chinese glossary phrase, preferably no more than 12 Chinese characters
                - exampleCn: a natural Chinese translation of the example sentence, or an empty string if exampleEn is empty

                Do not add markdown, code fences, explanations, or extra keys.
                """;
    }

    private String userPrompt(List<DictionaryTranslationRequest> requests) {
        try {
            return objectMapper.writeValueAsString(requests);
        } catch (Exception exception) {
            throw new IllegalArgumentException("无法构建翻译请求");
        }
    }

    private List<DictionaryTranslationItem> parseItems(String raw) throws Exception {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("(?s)^```(?:json)?\\s*", "");
            cleaned = cleaned.replaceFirst("(?s)\\s*```$", "");
        }
        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');
        String json = start >= 0 && end > start ? cleaned.substring(start, end + 1) : cleaned;
        DictionaryTranslationItem[] items = objectMapper.readValue(json, DictionaryTranslationItem[].class);
        return items == null ? List.of() : List.of(items);
    }

    private Map<String, DictionaryTranslationItem> fallbackBatch(List<DictionaryTranslationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Map.of();
        }
        Map<String, DictionaryTranslationItem> fallback = new LinkedHashMap<>();
        for (DictionaryTranslationRequest request : requests) {
            DictionaryTranslationItem item = fallbackItem(request);
            fallback.put(normalizeWord(request.word()), item);
        }
        return fallback;
    }

    private DictionaryTranslationItem fallbackItem(DictionaryTranslationRequest request) {
        String meaning = safeText(request.definitionEn());
        String example = safeText(request.exampleEn());
        return new DictionaryTranslationItem(normalizeWord(request.word()), meaning, example);
    }

    private DictionaryTranslationItem normalizeItem(DictionaryTranslationItem item) {
        return new DictionaryTranslationItem(
                normalizeWord(item.word()),
                trimOrFallback(item.meaningCn()),
                trimOrFallback(item.exampleCn())
        );
    }

    private String normalizeWord(String word) {
        return word == null ? "" : word.trim().toLowerCase();
    }

    private String trimOrFallback(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String safeText(String text) {
        return StringUtils.hasText(text) ? text.trim() : "";
    }
}
