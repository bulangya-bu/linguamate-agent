package com.example.javabigwork.vocabulary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.ai.dto.AiRuntimeSettings;
import com.example.javabigwork.ai.dto.OpenAiChatMessage;
import com.example.javabigwork.ai.service.AiSettingsService;
import com.example.javabigwork.ai.service.OpenAiCompatibleClient;
import com.example.javabigwork.vocabulary.dto.WordAiDetailView;
import com.example.javabigwork.vocabulary.entity.Word;
import com.example.javabigwork.vocabulary.entity.WordAiDetailCache;
import com.example.javabigwork.vocabulary.mapper.WordAiDetailCacheMapper;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WordAiDetailService {
    private static final String LOCAL_MODEL_NAME = "local-rules";

    private final WordMapper wordMapper;
    private final WordAiDetailCacheMapper cacheMapper;
    private final AiSettingsService settingsService;
    private final OpenAiCompatibleClient client;
    private final ObjectMapper objectMapper;

    public WordAiDetailService(
            WordMapper wordMapper,
            WordAiDetailCacheMapper cacheMapper,
            AiSettingsService settingsService,
            OpenAiCompatibleClient client,
            ObjectMapper objectMapper
    ) {
        this.wordMapper = wordMapper;
        this.cacheMapper = cacheMapper;
        this.settingsService = settingsService;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public WordAiDetailView detail(Long wordId) {
        Word word = wordMapper.selectById(wordId);
        if (word == null) {
            throw new IllegalArgumentException("单词不存在");
        }

        AiRuntimeSettings settings = settingsService.resolveCurrent();
        if (!StringUtils.hasText(settings.baseUrl())
                || !StringUtils.hasText(settings.chatModel())
                || !StringUtils.hasText(settings.apiKey())) {
            return localDetail(word);
        }

        String modelName = settings.chatModel().trim();
        WordAiDetailCache cached = findCache(wordId, modelName);
        if (cached != null && StringUtils.hasText(cached.contentJson)) {
            try {
                return withCacheFlag(parse(cached.contentJson), modelName, true);
            } catch (Exception ignored) {
                cacheMapper.deleteById(cached.id);
            }
        }

        try {
            String raw = client.chat(settings, systemPrompt(), List.of(new OpenAiChatMessage("user", userPrompt(word))));
            WordAiDetailView generated = withCacheFlag(parse(raw), modelName, false);
            WordAiDetailCache cache = new WordAiDetailCache();
            cache.wordId = wordId;
            cache.modelName = modelName;
            cache.contentJson = objectMapper.writeValueAsString(generated);
            cache.createdAt = LocalDateTime.now();
            cache.updatedAt = cache.createdAt;
            cacheMapper.insert(cache);
            return generated;
        } catch (Exception exception) {
            return localDetail(word);
        }
    }

    private WordAiDetailView localDetail(Word word) {
        String wordText = safe(word.word);
        String partOfSpeech = StringUtils.hasText(word.partOfSpeech) ? word.partOfSpeech.trim() : "unknown";
        String meaning = StringUtils.hasText(word.meaningCn) ? word.meaningCn.trim() : "请结合上下文理解含义";
        String exampleEn = WordExampleFallback.english(word);
        String exampleCn = WordExampleFallback.chinese(word);
        boolean placeholder=exampleEn.startsWith("The word \"") && exampleEn.endsWith("is useful in English practice.");
        return new WordAiDetailView(
                wordText,
                partOfSpeech,
                placeholder?List.of():List.of(new WordAiDetailView.Example(exampleEn, exampleCn)),
                List.of(new WordAiDetailView.Variant(wordText, partOfSpeech, meaning)),
                List.of(),
                List.of(),
                LOCAL_MODEL_NAME,
                false
        );
    }

    private WordAiDetailCache findCache(Long wordId, String modelName) {
        return cacheMapper.selectList(Wrappers.<WordAiDetailCache>query()
                        .eq("word_id", wordId)
                        .eq("model_name", modelName)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private String systemPrompt() {
        return """
                You are LinguaMate, an English vocabulary teacher.
                Return only valid JSON. Do not use markdown, code fences, or extra commentary.
                Keep explanations concise and useful for Chinese learners.

                Required JSON shape:
                {
                  "word": "target word",
                  "partOfSpeech": "main part of speech",
                  "examples": [
                    { "en": "Natural English sentence.", "cn": "中文翻译" }
                  ],
                  "variants": [
                    { "form": "word form", "type": "noun/verb/adjective/adverb/past tense/etc.", "meaningCn": "中文含义" }
                  ],
                  "usageNotes": ["中文用法提示"],
                  "collocations": [
                    { "phrase": "common phrase", "meaningCn": "中文含义", "exampleEn": "Short English example." }
                  ]
                }

                Constraints:
                - examples: 2 to 3 items.
                - variants: 1 to 5 useful items. If none, return [].
                - usageNotes: 2 to 4 short Chinese strings.
                - collocations: 2 to 5 items.
                - Do not include fields outside the required JSON shape.
                """;
    }

    private String userPrompt(Word word) {
        return """
                Target word: %s
                Part of speech from local word bank: %s
                Chinese meaning from local word bank: %s
                Existing example: %s

                Generate structured vocabulary detail for this word.
                """.formatted(
                safe(word.word),
                safe(word.partOfSpeech),
                safe(word.meaningCn),
                safe(word.exampleEn)
        );
    }

    private WordAiDetailView parse(String raw) throws Exception {
        String json = extractJson(raw);
        WordAiDetailView view = objectMapper.readValue(json, WordAiDetailView.class);
        validate(view);
        return view;
    }

    private String extractJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("模型返回内容为空");
        }
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("(?s)^```(?:json)?\\s*", "");
            cleaned = cleaned.replaceFirst("(?s)\\s*```$", "");
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1).trim();
        }
        return cleaned;
    }

    private void validate(WordAiDetailView view) {
        if (view == null || !StringUtils.hasText(view.word())) {
            throw new IllegalArgumentException("模型返回格式不完整");
        }
    }

    private WordAiDetailView withCacheFlag(WordAiDetailView view, String modelName, boolean cached) {
        return new WordAiDetailView(
                safe(view.word()),
                safe(view.partOfSpeech()),
                view.examples() == null ? List.of() : view.examples(),
                view.variants() == null ? List.of() : view.variants(),
                view.usageNotes() == null ? List.of() : view.usageNotes(),
                view.collocations() == null ? List.of() : view.collocations(),
                modelName,
                cached
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
