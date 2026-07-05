package com.example.javabigwork.vocabulary.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.javabigwork.vocabulary.dto.WordCardView;
import com.example.javabigwork.vocabulary.dto.WordBankPage;
import com.example.javabigwork.vocabulary.dto.WordBankCategory;
import com.example.javabigwork.vocabulary.entity.Word;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class WordBankService {
    private final WordMapper wordMapper;
    private final VocabularyService vocabularyService;

    public WordBankService(WordMapper wordMapper, VocabularyService vocabularyService) {
        this.wordMapper = wordMapper;
        this.vocabularyService = vocabularyService;
    }

    /** All local word bank categories with counts. */
    public List<WordBankCategory> categories() {
        List<WordBankCategory> result = new ArrayList<>();
        Map<String, String> labelMap = Map.of(
                "cet4-bank", "CET-4",
                "cet6-bank", "CET-6",
                "local-fast", "核心词"
        );
        for (String tag : List.of("cet4-bank", "cet6-bank", "local-fast")) {
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
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(10, size));

        QueryWrapper<Word> wrapper = new QueryWrapper<>();

        if (category != null && !category.isBlank()) {
            wrapper.eq("level_tag", category.trim());
        }

        if (search != null && !search.isBlank()) {
            String trimmed = search.trim();
            wrapper.and(w -> w
                    .likeRight("word", trimmed)
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
}
