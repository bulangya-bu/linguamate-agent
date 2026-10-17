package com.example.javabigwork.dictionary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.dictionary.dto.DictionaryLookupData;
import com.example.javabigwork.dictionary.dto.DictionaryTranslationItem;
import com.example.javabigwork.dictionary.dto.DictionaryTranslationRequest;
import com.example.javabigwork.vocabulary.dto.WordCardView;
import com.example.javabigwork.vocabulary.entity.Word;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import com.example.javabigwork.vocabulary.service.WordDefinitionService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OnlineWordService {
    private final WordMapper wordMapper;
    private final CommonWordPoolService wordPoolService;
    private final DictionaryApiService dictionaryApiService;
    private final DictionaryTranslationService translationService;
    private final WordDefinitionService definitionService;

    public OnlineWordService(
            WordMapper wordMapper,
            CommonWordPoolService wordPoolService,
            DictionaryApiService dictionaryApiService,
            DictionaryTranslationService translationService,
            WordDefinitionService definitionService
    ) {
        this.wordMapper = wordMapper;
        this.wordPoolService = wordPoolService;
        this.dictionaryApiService = dictionaryApiService;
        this.translationService = translationService;
        this.definitionService = definitionService;
    }

    public List<WordCardView> randomWordCards(int limit) {
        return randomWords(limit).stream().map(this::toView).toList();
    }

    public List<Word> randomWords(int limit) {
        int safeLimit = Math.max(1, Math.min(80, limit));
        List<Word> results = new ArrayList<>(cachedStudyWords(safeLimit));
        if (results.size() >= safeLimit) {
            return results.stream().limit(safeLimit).toList();
        }
        Set<String> seenWords = new LinkedHashSet<>();
        results.forEach(word -> seenWords.add(normalize(word.word)));
        int attempts = 0;
        while (results.size() < safeLimit && attempts < 3) {
            int remaining = safeLimit - results.size();
            int sampleSize = attempts == 0 ? safeLimit + 24 : Math.max(20, remaining + 12);
            List<String> seedWords = wordPoolService.sample(sampleSize)
                    .stream()
                    .filter(seenWords::add)
                    .toList();
            if (seedWords.isEmpty()) {
                break;
            }
            List<Word> batch = resolveWords(seedWords, remaining);
            for (Word word : batch) {
                if (word != null && seenWords.add("resolved:" + normalize(word.word))) {
                    results.add(word);
                }
            }
            attempts += 1;
        }
        return results.stream().limit(safeLimit).toList();
    }

    public List<Word> randomWords(int limit, String category) {
        if (!StringUtils.hasText(category)) {
            return randomWords(limit);
        }
        int safeLimit = Math.max(1, Math.min(80, limit));
        return wordMapper.selectList(Wrappers.<Word>query()
                .eq("level_tag", category.trim())
                .last("ORDER BY RAND() LIMIT " + safeLimit));
    }

    public Word resolveWord(String rawWord) {
        return resolveWord(rawWord, true);
    }

    public Word resolveWord(String rawWord, boolean enrichChinese) {
        String normalized = normalize(rawWord);
        if (!StringUtils.hasText(normalized)) {
            return fallbackWord(normalized);
        }
        List<Word> resolved = resolveWords(List.of(normalized), 1, enrichChinese);
        return resolved.isEmpty() ? fallbackWord(normalized) : resolved.get(0);
    }

    public List<Word> resolveWords(List<String> rawWords, int limit) {
        return resolveWords(rawWords, limit, true);
    }

    private List<Word> resolveWords(List<String> rawWords, int limit, boolean enrichChinese) {
        if (rawWords == null || rawWords.isEmpty()) {
            return List.of();
        }
        List<String> uniqueWords = rawWords.stream()
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        Map<String, Word> resolved = new LinkedHashMap<>();
        Map<String, Word> staleCached = new LinkedHashMap<>();
        List<String> misses = new ArrayList<>();
        for (String word : uniqueWords) {
            Word cached = findCached(word);
            if (isFreshCached(cached, enrichChinese)) {
                resolved.put(word, cached);
            } else {
                staleCached.put(word, cached);
                misses.add(word);
            }
        }

        List<CompletableFuture<DictionaryLookupData>> futures = misses.stream()
                .map(word -> CompletableFuture.supplyAsync(() -> dictionaryApiService.lookup(word).orElse(null)))
                .toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        List<DictionaryLookupData> lookups = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.hasText(item.definitionEn()))
                .toList();
        Map<String, DictionaryTranslationItem> translations = enrichChinese
                ? translationService.translateBatch(
                        lookups.stream()
                                .map(item -> new DictionaryTranslationRequest(
                                        item.word(),
                                        item.partOfSpeech(),
                                        item.definitionEn(),
                                        item.exampleEn()))
                                .toList())
                : Map.of();

        for (DictionaryLookupData lookup : lookups) {
            String key = normalize(lookup.word());
            Word cached = staleCached.get(key);
            Word word = merge(cached, lookup, translations.get(key));
            persist(word, cached == null);
            resolved.put(key, word);
        }

        return uniqueWords.stream()
                .map(resolved::get)
                .filter(Objects::nonNull)
                .limit(limit)
                .toList();
    }

    public WordCardView lookupView(String rawWord) {
        return toView(resolveWord(rawWord, true));
    }

    private Word findCached(String normalized) {
        return wordMapper.selectList(Wrappers.<Word>query().eq("word", normalized).last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private List<Word> cachedStudyWords(int limit) {
        int safeLimit = Math.max(1, limit);
        // CET-4 + CET-6 local word bank (fast, no network)
        List<Word> words = new ArrayList<>(wordMapper.selectList(Wrappers.<Word>query()
                .in("level_tag", List.of("cet4-bank", "cet6-bank"))
                .last("ORDER BY RAND() LIMIT " + safeLimit)));
        if (words.size() >= safeLimit) {
            return words;
        }
        // Fallback: hand-crafted local words
        words.addAll(wordMapper.selectList(Wrappers.<Word>query()
                .eq("level_tag", "local-fast")
                .last("ORDER BY RAND() LIMIT " + (safeLimit - words.size()))));
        if (words.size() >= safeLimit) {
            return words;
        }
        // Last resort: online dictionary lookup
        words.addAll(wordMapper.selectList(Wrappers.<Word>query()
                .eq("level_tag", "dictionaryapi.dev")
                .last("ORDER BY RAND() LIMIT " + (safeLimit - words.size()))));
        return words;
    }

    private boolean isFreshCached(Word cached, boolean enrichChinese) {
        return cached != null
                && StringUtils.hasText(cached.meaningCn)
                && (StringUtils.hasText(cached.phoneticUs) || StringUtils.hasText(cached.phoneticUk))
                && (isLocalCached(cached) || "dictionaryapi.dev".equalsIgnoreCase(cached.levelTag))
                && (!enrichChinese || !isEnglishOnly(cached.meaningCn));
    }

    private boolean isLocalCached(Word cached) {
        String tag = cached.levelTag;
        return "local-fast".equalsIgnoreCase(tag)
                || "cet4-bank".equalsIgnoreCase(tag)
                || "cet6-bank".equalsIgnoreCase(tag)
                || "kaoyan-bank".equalsIgnoreCase(tag)
                || "ielts-bank".equalsIgnoreCase(tag);
    }

    private boolean isEnglishOnly(String text) {
        return text != null && text.matches("^[\\p{ASCII}\\s\\p{Punct}]+$");
    }

    private Word merge(Word cached, DictionaryLookupData lookup, DictionaryTranslationItem translated) {
        Word word = cached == null ? new Word() : cached;
        word.word = lookup.word();
        word.phoneticUs = lookup.phoneticUs();
        word.phoneticUk = lookup.phoneticUk();
        word.partOfSpeech = lookup.partOfSpeech();
        word.exampleEn = lookup.exampleEn();
        word.exampleCn = "";
        word.meaningCn = lookup.definitionEn();
        word.levelTag = "dictionaryapi.dev";
        if (translated != null && StringUtils.hasText(translated.meaningCn())) {
            word.meaningCn = translated.meaningCn();
            word.exampleCn = translated.exampleCn();
        }
        return word;
    }

    private void persist(Word word, boolean insert) {
        if (word == null) {
            return;
        }
        if (insert) {
            wordMapper.insert(word);
        } else {
            wordMapper.updateById(word);
        }
    }

    private Word fallbackWord(String normalized) {
        Word word = new Word();
        word.word = normalized;
        word.phoneticUs = "/" + normalized + "/";
        word.phoneticUk = "/" + normalized + "/";
        word.meaningCn = "在线词典暂未收录";
        word.partOfSpeech = "unknown";
        word.exampleEn = "Please check the context to understand this word.";
        word.exampleCn = "请结合上下文理解这个单词。";
        word.levelTag = "online-fallback";
        return word;
    }

    private String normalize(String rawWord) {
        if (rawWord == null) {
            return "";
        }
        return rawWord.replaceAll("[^A-Za-z'-]", "").toLowerCase(Locale.ROOT);
    }

    public WordCardView toView(Word word) {
        if (word == null) {
            return null;
        }
        return new WordCardView(
                word.id,
                word.word,
                word.phoneticUs,
                word.phoneticUk,
                word.meaningCn,
                word.partOfSpeech,
                word.exampleEn,
                word.exampleCn,
                word.levelTag,
                0,
                0,
                false,
                definitionService.definitionsFor(word)
        );
    }
}
