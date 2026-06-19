package com.example.javabigwork.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.dictionary.service.OnlineWordService;
import com.example.javabigwork.news.dto.HoverWordResponse;
import com.example.javabigwork.vocabulary.entity.UserWordProgress;
import com.example.javabigwork.vocabulary.entity.Word;
import com.example.javabigwork.vocabulary.mapper.UserWordProgressMapper;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DictionaryTool {
    private static final Logger log = LoggerFactory.getLogger(DictionaryTool.class);
    private final WordMapper wordMapper;
    private final UserWordProgressMapper progressMapper;
    private final OnlineWordService onlineWordService;

    public DictionaryTool(
            WordMapper wordMapper,
            UserWordProgressMapper progressMapper,
            OnlineWordService onlineWordService
    ) {
        this.wordMapper = wordMapper;
        this.progressMapper = progressMapper;
        this.onlineWordService = onlineWordService;
    }

    public HoverWordResponse lookup(String rawWord) {
        String normalized = normalize(rawWord);
        if (!StringUtils.hasText(normalized)) {
            log.warn("DictionaryTool.lookup: empty input");
            throw new IllegalArgumentException("Please enter a word to look up.");
        }
        log.info("DictionaryTool.lookup: word={}", normalized);
        Word word = findFastWord(normalized);
        if (word == null || !isLocalWord(word)) {
            return new HoverWordResponse(normalized, "", "", "词库暂未收录", "", "", false);
        }
        return toHoverResponse(word);
    }

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
        if (word.id == null) {
            wordMapper.insert(word);
        }
        long existing = progressMapper.selectCount(Wrappers.<UserWordProgress>query().eq("word_id", word.id));
        if (existing == 0) {
            UserWordProgress progress = new UserWordProgress();
            progress.wordId = word.id;
            progress.familiarity = 1;
            progress.wrongCount = 1;
            progress.reviewCount = 0;
            progress.favorite = false;
            progress.lastReviewAt = LocalDateTime.now();
            progress.nextReviewAt = LocalDateTime.now().plusDays(1);
            progressMapper.insert(progress);
        }
        return toHoverResponse(word);
    }

    private Word findFastWord(String normalized) {
        for (String candidate : lookupCandidates(normalized)) {
            Word word = wordMapper.selectList(Wrappers.<Word>query()
                            .eq("word", candidate)
                            .last("LIMIT 1"))
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (word != null) {
                return word;
            }
        }
        return null;
    }

    private HoverWordResponse toHoverResponse(Word word) {
        boolean saved = word.id != null && progressMapper.selectCount(
                Wrappers.<UserWordProgress>query().eq("word_id", word.id)) > 0;
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

    private boolean isLocalWord(Word word) {
        if (word == null || word.meaningCn == null) return false;
        // Only show words that have Chinese meaning (contain non-ASCII characters)
        for (int i = 0; i < word.meaningCn.length(); i++) {
            if (word.meaningCn.charAt(i) > 127) return true;
        }
        return false;
    }

    private String normalize(String rawWord) {
        if (rawWord == null) {
            return "";
        }
        return rawWord.replaceAll("[^A-Za-z'-]", "").toLowerCase(Locale.ROOT);
    }

    private List<String> lookupCandidates(String normalized) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(normalized);
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
        }
        if (normalized.endsWith("ed") && normalized.length() > 4) {
            String stem = normalized.substring(0, normalized.length() - 2);
            candidates.add(stem);
            candidates.add(stem + "e");
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
