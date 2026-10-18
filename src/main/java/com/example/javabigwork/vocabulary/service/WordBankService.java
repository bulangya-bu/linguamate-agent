package com.example.javabigwork.vocabulary.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.javabigwork.vocabulary.dto.WordCardView;
import com.example.javabigwork.vocabulary.dto.WordBankPage;
import com.example.javabigwork.vocabulary.dto.WordBankCategory;
import com.example.javabigwork.vocabulary.dto.WordBankStats;
import com.example.javabigwork.vocabulary.entity.Word;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import com.example.javabigwork.auth.UserContext;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class WordBankService {
    private final WordMapper wordMapper;
    private final VocabularyService vocabularyService;
    private final UserContext userContext;

    public WordBankService(WordMapper wordMapper, VocabularyService vocabularyService, UserContext userContext) {
        this.wordMapper = wordMapper;
        this.vocabularyService = vocabularyService;
        this.userContext = userContext;
    }

    /** All local word bank categories with counts. */
    public List<WordBankCategory> categories() {
        List<WordBankCategory> result = new ArrayList<>();
        Map<String, String> labelMap = Map.of(
                "cet4-bank", "CET-4",
                "cet6-bank", "CET-6",
                "kaoyan-bank", "考研",
                "ielts-bank", "雅思",
                "local-fast", "核心词"
        );
        for (String tag : List.of("cet4-bank", "cet6-bank", "kaoyan-bank", "ielts-bank", "local-fast")) {
            Long count = wordMapper.selectCount(
                    new QueryWrapper<Word>().eq("level_tag", tag));
            if (count != null && count > 0) {
                result.add(new WordBankCategory(tag, labelMap.getOrDefault(tag, tag), count));
            }
        }
        return result;
    }

    /** Paginated word list with optional category filter and search. */
    public WordBankPage words(String category, String search, int page, int size) {
        return words(category, search, page, size, "all");
    }

    public WordBankPage words(String category, String search, int page, int size, String scope) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(10, size));

        QueryWrapper<Word> wrapper = scopeQuery(scope);

        if (category != null && !category.isBlank()) {
            wrapper.eq("level_tag", category.trim());
        }

        if (search != null && !search.isBlank()) {
            String trimmed = search.trim().toLowerCase(Locale.ROOT);
            wrapper.and(w -> w
                    .apply("LOWER(word) LIKE {0}", trimmed + "%")
                    .or()
                    .likeRight("meaning_cn", trimmed));
        }

        wrapper.orderByAsc("word");

        // Use MyBatis-Plus Page for proper pagination
        Page<Word> mpPage = new Page<>(safePage + 1, safeSize);
        Page<Word> result = wordMapper.selectPage(mpPage, wrapper);

        List<WordCardView> views = result.getRecords().stream()
                .map(vocabularyService::toView)
                .collect(Collectors.toList());

        return new WordBankPage(views, result.getTotal(), safePage, safeSize);
    }

    public WordBankStats stats() {
        return new WordBankStats(
                wordMapper.selectCount(scopeQuery("all")),
                wordMapper.selectCount(scopeQuery("review")),
                wordMapper.selectCount(scopeQuery("favorites")),
                wordMapper.selectCount(scopeQuery("mastered")),
                wordMapper.selectCount(scopeQuery("due"))
        );
    }

    private QueryWrapper<Word> scopeQuery(String scope) {
        QueryWrapper<Word> wrapper = new QueryWrapper<>();
        switch (scope == null ? "all" : scope) {
            case "all" -> { }
            case "due" -> wrapper.apply("id IN (SELECT word_id FROM user_word_progress WHERE user_id = {0} AND next_review_at <= {1})",
                    userContext.userId(),java.time.LocalDateTime.now());
            case "review" -> wrapper.apply(
                    "id IN (SELECT word_id FROM user_word_progress WHERE user_id = {0} AND wrong_count > 0)",
                    userContext.userId());
            case "favorites" -> wrapper.apply(
                    "id IN (SELECT word_id FROM user_word_progress WHERE user_id = {0} AND favorite = TRUE)",
                    userContext.userId());
            case "mastered" -> wrapper.apply(
                    "id IN (SELECT word_id FROM user_word_progress WHERE user_id = {0} AND familiarity >= 3 AND COALESCE(wrong_count, 0) = 0)",
                    userContext.userId());
            default -> throw new IllegalArgumentException("不支持的词库分组");
        }
        return wrapper;
    }
}
