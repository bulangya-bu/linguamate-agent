package com.example.javabigwork.vocabulary.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.javabigwork.auth.LoginActivityService;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.dictionary.service.OfflineDictionaryService;
import com.example.javabigwork.dictionary.service.OnlineWordService;
import com.example.javabigwork.vocabulary.dto.WordCardView;
import com.example.javabigwork.vocabulary.dto.WordDefinition;
import com.example.javabigwork.vocabulary.dto.WordQuizRequest;
import com.example.javabigwork.vocabulary.dto.WordQuizResult;
import com.example.javabigwork.vocabulary.dto.WordReviewRequest;
import com.example.javabigwork.vocabulary.entity.UserWordProgress;
import com.example.javabigwork.vocabulary.entity.Word;
import com.example.javabigwork.vocabulary.entity.WordReviewLog;
import com.example.javabigwork.vocabulary.mapper.UserWordProgressMapper;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import com.example.javabigwork.vocabulary.mapper.WordReviewLogMapper;
import java.time.LocalDateTime;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VocabularyService {
    private final OnlineWordService onlineWordService;
    private final WordMapper wordMapper;
    private final UserWordProgressMapper progressMapper;
    private final WordReviewLogMapper logMapper;
    private final ExperienceService experienceService;
    private final UserContext userContext;
    private final WordDefinitionService definitionService;
    private final OfflineDictionaryService dictionary;

    public VocabularyService(
            OnlineWordService onlineWordService,
            WordMapper wordMapper,
            UserWordProgressMapper progressMapper,
            WordReviewLogMapper logMapper,
            ExperienceService experienceService,
            UserContext userContext,
            WordDefinitionService definitionService,
            OfflineDictionaryService dictionary
    ) {
        this.onlineWordService = onlineWordService;
        this.wordMapper = wordMapper;
        this.progressMapper = progressMapper;
        this.logMapper = logMapper;
        this.experienceService = experienceService;
        this.userContext = userContext;
        this.definitionService = definitionService;
        this.dictionary = dictionary;
    }

    public List<WordCardView> todayWords(int limit) {
        int safeLimit = Math.max(1, Math.min(80, limit));
        return onlineWordService.randomWords(safeLimit)
                .stream()
                .map(this::toView)
                .toList();
    }

    public List<WordCardView> todayWords(int limit, String category) {
        int safeLimit = Math.max(1, Math.min(80, limit));
        return onlineWordService.randomWords(safeLimit, category)
                .stream()
                .map(this::toView)
                .toList();
    }

    public List<WordCardView> newWords(int limit, String category) {
        int safeLimit = Math.max(1, Math.min(80, limit));
        boolean hasCategory = StringUtils.hasText(category);
        return wordMapper.selectList(new QueryWrapper<Word>()
                        .eq(hasCategory, "level_tag", hasCategory ? category.trim() : null)
                        .apply("TRIM(meaning_cn) <> ''")
                        .apply("""
                                NOT EXISTS (SELECT 1 FROM word_review_log log
                                    WHERE log.user_id = {0} AND log.word_id = word.id
                                      AND log.review_type IN ('QUIZ', 'STRICT_REVIEW')
                                      AND log.created_at IS NOT NULL)
                                """, userContext.userId())
                        .last("ORDER BY CASE WHEN level_tag IN ('cet4-bank', 'cet6-bank') THEN 0 ELSE 1 END, RAND() LIMIT " + safeLimit))
                .stream()
                .map(this::toView)
                .toList();
    }

    public List<WordCardView> favoriteWords(int limit) {
        return progressMapper.selectList(new QueryWrapper<UserWordProgress>()
                        .eq("user_id", userContext.userId())
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
                        .eq("user_id", userContext.userId())
                        .ge("familiarity", 3)
                        .and(query -> query.eq("wrong_count", 0).or().isNull("wrong_count"))
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
        return recordQuiz(word,request.answer(),correct);
    }

    public WordQuizResult submitMissionQuiz(Long wordId,String answer,boolean correct) {
        Word word=wordMapper.selectById(wordId);
        if(word==null)throw new IllegalArgumentException("单词不存在");
        return recordQuiz(word,answer,correct);
    }

    private WordQuizResult recordQuiz(Word word,String answer,boolean correct) {
        Long wordId=word.id;
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
        log.userId = userContext.userId();
        log.wordId = wordId;
        log.answer = answer;
        log.correct = correct;
        log.reviewType = "QUIZ";
        log.createdAt = LocalDateTime.now(LoginActivityService.ZONE);
        logMapper.insert(log);

        experienceService.addXp(correct ? 10 : 3);

        return new WordQuizResult(
                wordId,
                correct,
                word.meaningCn,
                WordExampleFallback.english(word),
                WordExampleFallback.chinese(word),
                progress.familiarity,
                progress.wrongCount,
                correct ? "答对了，下次复习会稍后出现" : "已加入高优先级错词复习"
        );
    }

    public WordQuizResult submitStrictReview(Long wordId, WordReviewRequest request) {
        Word word = wordMapper.selectById(wordId);
        if (word == null) {
            throw new IllegalArgumentException("单词不存在");
        }
        boolean spellingCorrect = isSpellingCorrect(word, request.spellingAnswer());
        boolean meaningCorrect = acceptsReviewMeaning(definitionService.definitionsFor(word), request.meaningAnswer());
        boolean correct = spellingCorrect && meaningCorrect;
        LocalDateTime now = LocalDateTime.now();

        UserWordProgress progress = findOrCreateProgress(wordId);
        progress.reviewCount = value(progress.reviewCount) + 1;
        progress.lastReviewAt = now;
        if (correct) {
            progress.wrongCount = 0;
            progress.familiarity = Math.max(3, Math.min(5, value(progress.familiarity) + 1));
            progress.nextReviewAt = now.plusDays(Math.max(1, progress.familiarity));
        } else {
            progress.wrongCount = value(progress.wrongCount) + 1;
            progress.familiarity = Math.max(0, value(progress.familiarity) - 1);
            progress.nextReviewAt = now.plusHours(6);
        }
        progressMapper.updateById(progress);

        WordReviewLog log = new WordReviewLog();
        log.userId = userContext.userId();
        log.wordId = wordId;
        log.answer = joinReviewAnswer(request);
        log.spellingAnswer = request.spellingAnswer();
        log.meaningAnswer = request.meaningAnswer();
        log.correct = correct;
        log.spellingCorrect = spellingCorrect;
        log.meaningCorrect = meaningCorrect;
        log.reviewType = "STRICT_REVIEW";
        log.createdAt = LocalDateTime.now(LoginActivityService.ZONE);
        logMapper.insert(log);

        experienceService.addXp(correct ? 12 : 3);

        return new WordQuizResult(
                wordId,
                correct,
                word.word,
                word.meaningCn,
                WordExampleFallback.english(word),
                WordExampleFallback.chinese(word),
                progress.familiarity,
                progress.wrongCount,
                correct ? "本组已通过，错词已移出队列" : "未过关，已放回本组最后继续复习",
                spellingCorrect,
                meaningCorrect
        );
    }

    public WordCardView toView(Word word) {
        UserWordProgress progress = findProgress(word.id);
        String sourceSentence = progress == null ? null : progress.sourceSentence;
        String exampleEn = WordExampleFallback.english(word);
        return new WordCardView(
                word.id,
                word.word,
                word.phoneticUs,
                word.phoneticUk,
                word.meaningCn,
                word.partOfSpeech,
                exampleEn,
                WordExampleFallback.chinese(word),
                word.levelTag,
                progress == null ? 0 : value(progress.familiarity),
                progress == null ? 0 : value(progress.wrongCount),
                progress != null && Boolean.TRUE.equals(progress.favorite),
                definitionService.definitionsFor(word),
                sourceSentence,
                progress == null ? null : progress.sourceTitle,
                progress == null ? null : progress.sourceWord,
                progress == null ? null : progress.sourceArticleId,
                dictionary.inflectionsInExamples(word.word, sourceSentence, exampleEn)
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

    private boolean isSpellingCorrect(Word word, String answer) {
        String trimmed = answer == null ? "" : answer.trim();
        return !trimmed.isBlank() && word.word.equalsIgnoreCase(trimmed);
    }

    static boolean acceptsReviewMeaning(List<WordDefinition> definitions, String answer) {
        List<String> supplied = meaningParts(answer);
        if (supplied.isEmpty()) return false;
        Set<String> accepted = new LinkedHashSet<>();
        for (WordDefinition definition : definitions) {
            for (String sense : meaningParts(definition.meaningCn())) {
                accepted.add(sense);
                // Subject labels and parenthetical usage notes are optional in dictionary answers.
                String plain = sense.replaceAll("\\[[^\\]]*\\]|【[^】]*】|\\([^)]*\\)", "");
                if (!plain.isEmpty()) {
                    accepted.add(plain);
                    if (plain.endsWith("的") && plain.length() >= 3) {
                        accepted.add(plain.substring(0, plain.length() - 1));
                    }
                }
            }
        }
        return accepted.containsAll(supplied);
    }

    private static List<String> meaningParts(String value) {
        if (value == null || value.isBlank()) return List.of();
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", "").replaceAll("[。.!！]+$", "");
        return Arrays.stream(normalized.split("[,;、/|]+(?![^()]*\\))"))
                .filter(sense -> !sense.isEmpty()).toList();
    }

    private String joinReviewAnswer(WordReviewRequest request) {
        return "%s | %s".formatted(
                request.spellingAnswer() == null ? "" : request.spellingAnswer().trim(),
                request.meaningAnswer() == null ? "" : request.meaningAnswer().trim()
        );
    }

    private UserWordProgress findProgress(Long wordId) {
        return progressMapper.selectList(new QueryWrapper<UserWordProgress>()
                        .eq("user_id", userContext.userId())
                        .eq("word_id", wordId)
                        .last("LIMIT 1"))
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
        progress.userId = userContext.userId();
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
