package com.example.javabigwork.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.cache.RedisCacheService;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.dictionary.service.OnlineWordService;
import com.example.javabigwork.dictionary.service.OfflineDictionaryService;
import com.example.javabigwork.news.dto.HoverWordResponse;
import com.example.javabigwork.vocabulary.entity.UserWordProgress;
import com.example.javabigwork.vocabulary.entity.Word;
import com.example.javabigwork.vocabulary.mapper.UserWordProgressMapper;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class DictionaryTool {
    private static final Logger log = LoggerFactory.getLogger(DictionaryTool.class);
    private static final Duration WORD_LOOKUP_TTL = Duration.ofHours(6);

    private final WordMapper wordMapper;
    private final UserWordProgressMapper progressMapper;
    private final OnlineWordService onlineWordService;
    private final UserContext userContext;
    private final RedisCacheService redisCache;
    private final OfflineDictionaryService offlineDictionary;

    public DictionaryTool(
            WordMapper wordMapper,
            UserWordProgressMapper progressMapper,
            OnlineWordService onlineWordService,
            UserContext userContext,
            RedisCacheService redisCache,
            OfflineDictionaryService offlineDictionary
    ) {
        this.wordMapper = wordMapper;
        this.progressMapper = progressMapper;
        this.onlineWordService = onlineWordService;
        this.userContext = userContext;
        this.redisCache = redisCache;
        this.offlineDictionary = offlineDictionary;
    }

    public HoverWordResponse lookup(String rawWord) {
        String normalized = normalize(rawWord);
        if (!StringUtils.hasText(normalized)) {
            log.warn("DictionaryTool.lookup: empty input");
            throw new IllegalArgumentException("Please enter a word to look up.");
        }
        log.info("DictionaryTool.lookup: word={}", normalized);
        Word word = findFastWord(normalized);
        if (!hasReliableDefinition(word)) {
            return new HoverWordResponse(normalized, "", "", "词库暂未收录", "", "", false);
        }
        return toHoverResponse(word);
    }

    @Transactional
    public HoverWordResponse saveToReviewBook(String rawWord) {
        String normalized = normalize(rawWord);
        if (!StringUtils.hasText(normalized)) {
            log.warn("DictionaryTool.saveToReviewBook: empty input");
            throw new IllegalArgumentException("Please enter a word to look up.");
        }
        log.info("DictionaryTool.saveToReviewBook: word={}", normalized);
        Word word = findFastWord(normalized);
        if (word == null) {
            word = onlineWordService.resolveWord(normalized, false);
        }
        if (!hasReliableDefinition(word)) {
            throw new IllegalArgumentException("暂未查到可靠释义，请检查拼写或换用单词原形");
        }
        if (word.id == null) {
            wordMapper.insert(word);
        } else if (OfflineDictionaryService.SOURCE_TAG.equals(word.levelTag)) {
            // Repair an existing incomplete online entry when the learner saves its local definition.
            wordMapper.updateById(word);
        }
        cacheWordLookup(normalized, word);
        UserWordProgress existing = progressMapper.selectOne(Wrappers.<UserWordProgress>query()
                .eq("user_id", userContext.userId())
                .eq("word_id", word.id));
        if (existing == null) {
            UserWordProgress progress = new UserWordProgress();
            progress.userId = userContext.userId();
            progress.wordId = word.id;
            progress.familiarity = 1;
            progress.wrongCount = 1;
            progress.reviewCount = 0;
            progress.favorite = false;
            progress.nextReviewAt = LocalDateTime.now().withNano(0);
            progressMapper.insert(progress);
        } else if (existing.wrongCount == null || existing.wrongCount <= 0) {
            // Re-adding a learned or favorite word must actually put it in the review queue.
            existing.wrongCount = 1;
            existing.familiarity = 1;
            existing.nextReviewAt = LocalDateTime.now().withNano(0);
            progressMapper.updateById(existing);
        }
        return toHoverResponse(word);
    }

    private Word findFastWord(String normalized) {
        String cacheKey = wordLookupKey(normalized);
        Word cached = redisCache.get(cacheKey, Word.class).orElse(null);
        if (hasChineseDefinition(cached)) {
            return cached;
        }
        Word englishFallback = null;
        for (String candidate : lookupCandidates(normalized)) {
            Word word = findStoredWord(candidate);
            if (hasChineseDefinition(word)) {
                cacheWordLookup(normalized, word);
                return word;
            }
            Word supplement = offlineDictionary.lookup(candidate).orElse(null);
            if (supplement != null) {
                Word stored = supplement.word.equals(candidate) ? word : findStoredWord(supplement.word);
                if (hasChineseDefinition(stored)) supplement = stored;
                else if (stored != null) {
                    supplement.id = stored.id;
                    supplement.exampleEn = stored.exampleEn;
                    supplement.exampleCn = stored.exampleCn;
                }
                cacheWordLookup(normalized, supplement);
                return supplement;
            }
            if (englishFallback == null && hasReliableDefinition(word)) englishFallback = word;
        }
        return englishFallback;
    }

    private Word findStoredWord(String normalized) {
        return wordMapper.selectList(Wrappers.<Word>query().eq("word", normalized).last("LIMIT 1"))
                .stream().findFirst().orElse(null);
    }

    private void cacheWordLookup(String normalized, Word word) {
        if (word == null || word.id == null) {
            return;
        }
        redisCache.putIndexed(wordLookupIndexKey(), wordLookupKey(normalized), word, WORD_LOOKUP_TTL);
    }

    private String wordLookupKey(String normalized) {
        return redisCache.key("word:lookup:v2", normalized);
    }

    private String wordLookupIndexKey() {
        return redisCache.key("word:lookup:index");
    }

    private HoverWordResponse toHoverResponse(Word word) {
        boolean saved = word.id != null && progressMapper.selectCount(
                Wrappers.<UserWordProgress>query()
                        .eq("user_id", userContext.userId())
                        .eq("word_id", word.id)
                        .gt("wrong_count", 0)) > 0;
        return new HoverWordResponse(
                word.word,
                word.phoneticUs,
                word.phoneticUk,
                word.meaningCn,
                word.partOfSpeech,
                word.exampleEn,
                saved
        );
    }

    private boolean hasChineseDefinition(Word word) {
        return hasReliableDefinition(word) && word.meaningCn.matches("(?s).*[\\p{IsHan}].*");
    }

    private boolean hasReliableDefinition(Word word) {
        return word != null && StringUtils.hasText(word.meaningCn)
                && !"online-fallback".equals(word.levelTag)
                && !word.meaningCn.contains("暂未收录")
                && !word.meaningCn.contains("查询失败");
    }

    private String normalize(String rawWord) {
        if (rawWord == null) {
            return "";
        }
        String word = rawWord.replace('’', '\'').replace('‘', '\'').replace('‑', '-').replace('‐', '-')
                .trim().replaceAll("^[^A-Za-z]+|[^A-Za-z]+$", "").toLowerCase(Locale.ROOT);
        return word.length() <= 64 && word.matches("[a-z]+(?:['-][a-z]+)*") ? word : "";
    }

    private List<String> lookupCandidates(String normalized) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(normalized);
        if (normalized.endsWith("'s")) candidates.add(normalized.substring(0, normalized.length() - 2));
        switch (normalized) {
            case "habits" -> candidates.add("habit");
            case "stories" -> candidates.add("story");
            case "conversations" -> candidates.add("conversation");
            case "meanings" -> candidates.add("meaning");
            case "examples" -> candidates.add("example");
            case "strategies" -> candidates.add("strategy");
            case "studies" -> candidates.add("study");
            case "tries" -> candidates.add("try");
            case "uses", "used", "using" -> candidates.add("use");
            case "gives", "gave", "given" -> candidates.add("give");
            case "becomes", "became", "becoming" -> candidates.add("become");
            case "chooses", "chose", "chosen", "choosing" -> candidates.add("choose");
            case "writes", "wrote", "written", "writing" -> candidates.add("write");
            case "reads", "reading" -> candidates.add("read");
            case "speaks", "spoke", "spoken", "speaking" -> candidates.add("speak");
            case "learns", "learned", "learning" -> candidates.add("learn");
            case "remembers", "remembered", "remembering" -> candidates.add("remember");
            case "improves", "improved", "improving" -> candidates.add("improve");
            case "connects", "connected", "connecting" -> candidates.add("connect");
            case "notices", "noticed", "noticing" -> candidates.add("notice");
            case "repeats", "repeated", "repeating" -> candidates.add("repeat");
            case "helps", "helped", "helping" -> candidates.add("help");
            case "supports", "supported", "supporting" -> candidates.add("support");
            default -> {
            }
        }
        if (normalized.endsWith("ies") && normalized.length() > 4) {
            candidates.add(normalized.substring(0, normalized.length() - 3) + "y");
        }
        if (normalized.endsWith("ing") && normalized.length() > 5) {
            String stem = normalized.substring(0, normalized.length() - 3);
            candidates.add(stem);
            candidates.add(stem + "e");
            if (stem.matches(".*([b-df-hj-np-tv-z])\\1$")) candidates.add(stem.substring(0, stem.length() - 1));
        }
        if (normalized.endsWith("ed") && normalized.length() > 4) {
            String stem = normalized.substring(0, normalized.length() - 2);
            candidates.add(stem);
            candidates.add(stem + "e");
            if (stem.matches(".*([b-df-hj-np-tv-z])\\1$")) candidates.add(stem.substring(0, stem.length() - 1));
        }
        if (normalized.endsWith("es") && normalized.length() > 4) {
            candidates.add(normalized.substring(0, normalized.length() - 2));
        }
        if (normalized.endsWith("s") && normalized.length() > 3) {
            candidates.add(normalized.substring(0, normalized.length() - 1));
        }
        return new ArrayList<>(candidates);
    }
}
