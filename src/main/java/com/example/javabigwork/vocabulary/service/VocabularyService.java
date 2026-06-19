package com.example.javabigwork.vocabulary.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.javabigwork.dictionary.service.OnlineWordService;
import com.example.javabigwork.vocabulary.dto.WordCardView;
import com.example.javabigwork.vocabulary.dto.WordQuizRequest;
import com.example.javabigwork.vocabulary.dto.WordQuizResult;
import com.example.javabigwork.vocabulary.entity.UserWordProgress;
import com.example.javabigwork.vocabulary.entity.Word;
import com.example.javabigwork.vocabulary.entity.WordReviewLog;
import com.example.javabigwork.vocabulary.mapper.UserWordProgressMapper;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import com.example.javabigwork.vocabulary.mapper.WordReviewLogMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VocabularyService {
    private final OnlineWordService onlineWordService;
    private final WordMapper wordMapper;
    private final UserWordProgressMapper progressMapper;
    private final WordReviewLogMapper logMapper;
    private final ExperienceService experienceService;

    public VocabularyService(
            OnlineWordService onlineWordService,
            WordMapper wordMapper,
            UserWordProgressMapper progressMapper,
            WordReviewLogMapper logMapper,
            ExperienceService experienceService
    ) {
        this.onlineWordService = onlineWordService;
        this.wordMapper = wordMapper;
        this.progressMapper = progressMapper;
        this.logMapper = logMapper;
        this.experienceService = experienceService;
    }

    public List<WordCardView> todayWords(int limit) {
        int safeLimit = Math.max(1, Math.min(80, limit));
        return onlineWordService.randomWords(safeLimit)
                .stream()
                .map(this::toView)
                .toList();
    }

    public List<WordCardView> favoriteWords(int limit) {
        return progressMapper.selectList(new QueryWrapper<UserWordProgress>()
                        .eq("favorite", true)
                        .orderByDesc("last_review_at")
                        .orderByDesc("id")
                        .last("LIMIT " + Math.max(1, limit)))
                .stream()
                .map(progress -> {
                    Word word = wordMapper.selectById(progress.wordId);
                    return word == null ? null : toView(word);
                })
                .filter(item -> item != null)
                .toList();
    }

    public List<WordCardView> masteredWords(int limit) {
        return progressMapper.selectList(new QueryWrapper<UserWordProgress>()
                        .ge("familiarity", 3)
                        .orderByDesc("familiarity")
                        .orderByDesc("last_review_at")
                        .orderByDesc("id")
                        .last("LIMIT " + Math.max(1, limit)))
                .stream()
                .map(progress -> {
                    Word word = wordMapper.selectById(progress.wordId);
                    return word == null ? null : toView(word);
                })
                .filter(item -> item != null)
                .toList();
    }

    public WordCardView toggleFavorite(Long wordId) {
        Word word = wordMapper.selectById(wordId);
        if (word == null) {
            throw new IllegalArgumentException("单词不存在");
        }
        UserWordProgress progress = findOrCreateProgress(wordId);
        progress.favorite = !Boolean.TRUE.equals(progress.favorite);
        progress.lastReviewAt = LocalDateTime.now();
        progressMapper.updateById(progress);
        return toView(word);
    }

    public WordQuizResult submitQuiz(Long wordId, WordQuizRequest request) {
        Word word = wordMapper.selectById(wordId);
        if (word == null) {
            throw new IllegalArgumentException("单词不存在");
        }
        boolean correct = isCorrect(word, request.answer());
        UserWordProgress progress = findOrCreateProgress(wordId);
        progress.reviewCount = value(progress.reviewCount) + 1;
        progress.lastReviewAt = LocalDateTime.now();
        if (correct) {
            progress.familiarity = Math.min(5, value(progress.familiarity) + 1);
            progress.nextReviewAt = LocalDateTime.now().plusDays(Math.max(1, progress.familiarity));
        } else {
            progress.wrongCount = value(progress.wrongCount) + 1;
            progress.familiarity = Math.max(0, value(progress.familiarity) - 1);
            progress.nextReviewAt = LocalDateTime.now().plusHours(6);
        }
        progressMapper.updateById(progress);

        WordReviewLog log = new WordReviewLog();
        log.wordId = wordId;
        log.answer = request.answer();
        log.correct = correct;
        log.createdAt = LocalDateTime.now();
        logMapper.insert(log);

        experienceService.addXp(correct ? 10 : 3);

        return new WordQuizResult(
                wordId,
                correct,
                word.meaningCn,
                progress.familiarity,
                progress.wrongCount,
                correct ? "答对了，下次复习会稍后出现" : "已加入高优先级错词复习"
        );
    }

    public WordCardView toView(Word word) {
        UserWordProgress progress = findProgress(word.id);
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
                progress == null ? 0 : value(progress.familiarity),
                progress == null ? 0 : value(progress.wrongCount),
                progress != null && Boolean.TRUE.equals(progress.favorite)
        );
    }

    public Word getWord(Long wordId) {
        return wordMapper.selectById(wordId);
    }

    private boolean isCorrect(Word word, String answer) {
        String trimmed = answer == null ? "" : answer.trim();
        return !trimmed.isBlank()
                && (word.word.equalsIgnoreCase(trimmed) || (word.meaningCn != null && word.meaningCn.contains(trimmed)));
    }

    private UserWordProgress findProgress(Long wordId) {
        return progressMapper.selectList(new QueryWrapper<UserWordProgress>().eq("word_id", wordId).last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private UserWordProgress findOrCreateProgress(Long wordId) {
        UserWordProgress progress = findProgress(wordId);
        if (progress != null) {
            return progress;
        }
        progress = new UserWordProgress();
        progress.wordId = wordId;
        progress.familiarity = 0;
        progress.wrongCount = 0;
        progress.reviewCount = 0;
        progress.favorite = false;
        progress.lastReviewAt = LocalDateTime.now();
        progress.nextReviewAt = LocalDateTime.now();
        progressMapper.insert(progress);
        return progress;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
