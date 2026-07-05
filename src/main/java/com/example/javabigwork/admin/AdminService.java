package com.example.javabigwork.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.javabigwork.ai.entity.AiSetting;
import com.example.javabigwork.ai.mapper.AiSettingMapper;
import com.example.javabigwork.auth.AuthService;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.auth.entity.UserAccount;
import com.example.javabigwork.auth.mapper.UserAccountMapper;
import com.example.javabigwork.chat.entity.Conversation;
import com.example.javabigwork.chat.mapper.ConversationMapper;
import com.example.javabigwork.knowledge.mapper.KnowledgeDocumentMapper;
import com.example.javabigwork.news.entity.NewsArticle;
import com.example.javabigwork.news.entity.ReadingRecord;
import com.example.javabigwork.news.mapper.NewsArticleMapper;
import com.example.javabigwork.news.mapper.ReadingRecordMapper;
import com.example.javabigwork.vocabulary.entity.UserWordProgress;
import com.example.javabigwork.vocabulary.entity.Word;
import com.example.javabigwork.vocabulary.entity.WordReviewLog;
import com.example.javabigwork.vocabulary.entity.UserLevel;
import com.example.javabigwork.vocabulary.mapper.UserLevelMapper;
import com.example.javabigwork.vocabulary.mapper.UserWordProgressMapper;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import com.example.javabigwork.vocabulary.mapper.WordReviewLogMapper;
import com.example.javabigwork.writing.entity.EssaySubmission;
import com.example.javabigwork.writing.mapper.EssaySubmissionMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserAccountMapper userMapper;
    private final WordMapper wordMapper;
    private final NewsArticleMapper newsArticleMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final EssaySubmissionMapper essaySubmissionMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final UserWordProgressMapper progressMapper;
    private final WordReviewLogMapper reviewLogMapper;
    private final UserLevelMapper userLevelMapper;
    private final AiSettingMapper aiSettingMapper;
    private final ConversationMapper conversationMapper;
    private final AuthService authService;
    private final UserContext userContext;

    public AdminService(
            UserAccountMapper userMapper,
            WordMapper wordMapper,
            NewsArticleMapper newsArticleMapper,
            KnowledgeDocumentMapper knowledgeDocumentMapper,
            EssaySubmissionMapper essaySubmissionMapper,
            ReadingRecordMapper readingRecordMapper,
            UserWordProgressMapper progressMapper,
            WordReviewLogMapper reviewLogMapper,
            UserLevelMapper userLevelMapper,
            AiSettingMapper aiSettingMapper,
            ConversationMapper conversationMapper,
            AuthService authService,
            UserContext userContext
    ) {
        this.userMapper = userMapper;
        this.wordMapper = wordMapper;
        this.newsArticleMapper = newsArticleMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.essaySubmissionMapper = essaySubmissionMapper;
        this.readingRecordMapper = readingRecordMapper;
        this.progressMapper = progressMapper;
        this.reviewLogMapper = reviewLogMapper;
        this.userLevelMapper = userLevelMapper;
        this.aiSettingMapper = aiSettingMapper;
        this.conversationMapper = conversationMapper;
        this.authService = authService;
        this.userContext = userContext;
    }

    public AdminSummary summary() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        List<WordReviewLog> todayLogs = reviewLogMapper.selectList(Wrappers.<WordReviewLog>query()
                .ge("created_at", todayStart)
                .lt("created_at", tomorrowStart));
        long todayWordReviews = todayLogs.size();
        long todayLearners = todayLogs.stream()
                .map(log -> log.userId)
                .filter(id -> id != null)
                .distinct()
                .count();
        long todayUniqueWords = todayLogs.stream()
                .map(log -> log.wordId)
                .filter(id -> id != null)
                .distinct()
                .count();
        List<DailyWordStat> dailyWordStats = dailyWordStats(today.minusDays(6), tomorrowStart);
        return new AdminSummary(
                userMapper.selectCount(Wrappers.<UserAccount>query()),
                userMapper.selectCount(Wrappers.<UserAccount>query().eq("status", "ACTIVE")),
                wordMapper.selectCount(Wrappers.<Word>query()),
                newsArticleMapper.selectCount(Wrappers.<NewsArticle>query()),
                knowledgeDocumentMapper.selectCount(Wrappers.query()),
                essaySubmissionMapper.selectCount(Wrappers.query()),
                readingRecordMapper.selectCount(Wrappers.query()),
                progressMapper.selectCount(Wrappers.<UserWordProgress>query().gt("wrong_count", 0)),
                conversationMapper.selectCount(Wrappers.<Conversation>query()),
                todayWordReviews,
                todayUniqueWords,
                todayLearners,
                dailyWordStats
        );
    }

    private List<DailyWordStat> dailyWordStats(LocalDate startDate, LocalDateTime endExclusive) {
        Map<LocalDate, Long> totals = new LinkedHashMap<>();
        for (int offset = 0; offset < 7; offset++) {
            totals.put(startDate.plusDays(offset), 0L);
        }
        reviewLogMapper.selectList(Wrappers.<WordReviewLog>query()
                        .ge("created_at", startDate.atStartOfDay())
                        .lt("created_at", endExclusive))
                .forEach(log -> {
                    if (log.createdAt != null) {
                        LocalDate day = log.createdAt.toLocalDate();
                        if (totals.containsKey(day)) {
                            totals.put(day, totals.get(day) + 1);
                        }
                    }
                });
        List<DailyWordStat> stats = new ArrayList<>();
        totals.forEach((date, total) -> stats.add(new DailyWordStat(date.toString(), total)));
        return stats;
    }

    public List<AdminUserView> users(String search) {
        QueryWrapper<UserAccount> wrapper = Wrappers.<UserAccount>query();
        if (search != null && !search.isBlank()) {
            String value = search.trim();
            wrapper.and(w -> w.like("username", value).or().like("nickname", value));
        }
        wrapper.orderByDesc("id").last("LIMIT 100");
        return userMapper.selectList(wrapper).stream().map(AdminUserView::from).toList();
    }

    public AdminUserProfile userProfile(Long userId) {
        UserAccount user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        UserLevel level = userLevelMapper.selectOne(Wrappers.<UserLevel>query()
                .eq("user_id", userId)
                .last("LIMIT 1"));
        AiSetting aiSetting = aiSettingMapper.selectOne(Wrappers.<AiSetting>query()
                .eq("user_id", userId)
                .last("LIMIT 1"));
        long learnedWords = progressMapper.selectCount(Wrappers.<UserWordProgress>query().eq("user_id", userId));
        long favoriteWords = progressMapper.selectCount(Wrappers.<UserWordProgress>query().eq("user_id", userId).eq("favorite", true));
        long masteredWords = progressMapper.selectCount(Wrappers.<UserWordProgress>query().eq("user_id", userId).ge("familiarity", 4));
        long reviewWords = progressMapper.selectCount(Wrappers.<UserWordProgress>query().eq("user_id", userId).gt("wrong_count", 0));
        long dueWords = progressMapper.selectCount(Wrappers.<UserWordProgress>query()
                .eq("user_id", userId)
                .le("next_review_at", LocalDateTime.now()));
        long reviewLogs = reviewLogMapper.selectCount(Wrappers.<WordReviewLog>query().eq("user_id", userId));
        long wrongReviewLogs = reviewLogMapper.selectCount(Wrappers.<WordReviewLog>query()
                .eq("user_id", userId)
                .eq("correct", false));
        long readings = readingRecordMapper.selectCount(Wrappers.<ReadingRecord>query().eq("user_id", userId));
        long finishedReadings = readingRecordMapper.selectCount(Wrappers.<ReadingRecord>query()
                .eq("user_id", userId)
                .eq("finished", true));
        long lookedUpWords = readingRecordMapper.selectList(Wrappers.<ReadingRecord>query().eq("user_id", userId))
                .stream()
                .mapToLong(record -> record.lookedUpWords == null ? 0 : record.lookedUpWords)
                .sum();
        return new AdminUserProfile(
                user.id,
                user.username,
                user.nickname,
                user.role,
                user.status,
                level == null || level.level == null ? 1 : level.level,
                level == null || level.experience == null ? 0 : level.experience,
                level == null || level.totalExperience == null ? 0 : level.totalExperience,
                favoriteWords,
                learnedWords,
                masteredWords,
                reviewWords,
                dueWords,
                reviewLogs,
                wrongReviewLogs,
                essaySubmissionMapper.selectCount(Wrappers.<EssaySubmission>query().eq("user_id", userId)),
                readings,
                finishedReadings,
                lookedUpWords,
                conversationMapper.selectCount(Wrappers.<Conversation>query().eq("user_id", userId)),
                new VocabularyDistribution(learnedWords, masteredWords, reviewWords, dueWords, favoriteWords),
                new ReadingOverview(readings, finishedReadings, lookedUpWords),
                aiSetting != null && (hasText(aiSetting.baseUrl) || hasText(aiSetting.apiKey) || hasText(aiSetting.chatModel))
        );
    }

    @Transactional
    public AdminUserView updateUser(Long userId, UserUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("用户更新内容不能为空");
        }
        UserAccount user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if ("ADMIN".equals(user.role)) {
            throw new IllegalArgumentException("管理员账号不允许由后台编辑");
        }
        if (request.nickname() != null) {
            authService.updateNickname(userId, request.nickname());
        }
        if (request.status() != null && !request.status().isBlank()) {
            updateUserStatus(userId, new UserStatusRequest(request.status()));
        }
        if (request.password() != null && !request.password().isBlank()) {
            resetPassword(userId, new PasswordResetRequest(request.password()));
        }
        return AdminUserView.from(userMapper.selectById(userId));
    }

    public void updateUserStatus(Long userId, UserStatusRequest request) {
        UserAccount user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if ("ADMIN".equals(user.role) && "DISABLED".equalsIgnoreCase(request.status())) {
            throw new IllegalArgumentException("管理员账号不能禁用");
        }
        if (userContext.userId().equals(userId) && "DISABLED".equalsIgnoreCase(request.status())) {
            throw new IllegalArgumentException("不能禁用当前登录账号");
        }
        authService.updateStatus(userId, request.status());
    }

    public void resetPassword(Long userId, PasswordResetRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("密码不能为空");
        }
        UserAccount user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if ("ADMIN".equals(user.role)) {
            throw new IllegalArgumentException("管理员账号不允许由后台重置");
        }
        authService.resetPassword(userId, request.password());
    }

    public List<AdminReadingRecordView> userReadings(Long userId) {
        UserAccount user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return readingRecordMapper.selectList(Wrappers.<ReadingRecord>query()
                        .eq("user_id", userId)
                        .orderByDesc("created_at")
                        .last("LIMIT 100"))
                .stream()
                .map(record -> AdminReadingRecordView.from(record, newsArticleMapper.selectById(record.articleId)))
                .toList();
    }

    public AdminWordPage words(String category, String search, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(80, Math.max(10, size));
        QueryWrapper<Word> wrapper = Wrappers.<Word>query();
        if (category != null && !category.isBlank()) {
            wrapper.eq("level_tag", category.trim());
        }
        if (search != null && !search.isBlank()) {
            String value = search.trim();
            wrapper.and(w -> w.likeRight("word", value).or().likeRight("meaning_cn", value));
        }
        wrapper.orderByAsc("word");
        Page<Word> result = wordMapper.selectPage(new Page<>(safePage + 1L, safeSize), wrapper);
        return new AdminWordPage(result.getRecords(), result.getTotal(), safePage, safeSize);
    }

    @Transactional
    public Word createWord(WordUpsertRequest request) {
        Word word = new Word();
        applyWord(word, request);
        wordMapper.insert(word);
        return word;
    }

    @Transactional
    public Word updateWord(Long id, WordUpsertRequest request) {
        Word word = wordMapper.selectById(id);
        if (word == null) {
            throw new IllegalArgumentException("单词不存在");
        }
        applyWord(word, request);
        wordMapper.updateById(word);
        return word;
    }

    public void deleteWord(Long id) {
        wordMapper.deleteById(id);
    }

    public AdminNewsPage news(String search, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(10, size));
        QueryWrapper<NewsArticle> wrapper = Wrappers.<NewsArticle>query();
        if (search != null && !search.isBlank()) {
            String value = search.trim();
            wrapper.and(w -> w.like("title", value).or().like("source_name", value).or().like("topic_tags", value));
        }
        wrapper.orderByDesc("created_at").orderByDesc("id");
        Page<NewsArticle> result = newsArticleMapper.selectPage(new Page<>(safePage + 1L, safeSize), wrapper);
        return new AdminNewsPage(result.getRecords(), result.getTotal(), safePage, safeSize);
    }

    @Transactional
    public NewsArticle createNews(NewsUpsertRequest request) {
        NewsArticle article = new NewsArticle();
        applyNews(article, request);
        article.createdAt = LocalDateTime.now();
        newsArticleMapper.insert(article);
        return article;
    }

    @Transactional
    public NewsArticle updateNews(Long id, NewsUpsertRequest request) {
        NewsArticle article = newsArticleMapper.selectById(id);
        if (article == null) {
            throw new IllegalArgumentException("阅读文章不存在");
        }
        applyNews(article, request);
        newsArticleMapper.updateById(article);
        return article;
    }

    public void deleteNews(Long id) {
        newsArticleMapper.deleteById(id);
    }

    private void applyWord(Word word, WordUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("单词内容不能为空");
        }
        if (request.word() == null || request.word().isBlank()) {
            throw new IllegalArgumentException("英文单词不能为空");
        }
        word.word = request.word().trim();
        word.phoneticUs = trim(request.phoneticUs());
        word.phoneticUk = trim(request.phoneticUk());
        word.meaningCn = trim(request.meaningCn());
        word.partOfSpeech = trim(request.partOfSpeech());
        word.exampleEn = trim(request.exampleEn());
        word.exampleCn = trim(request.exampleCn());
        word.levelTag = request.levelTag() == null || request.levelTag().isBlank() ? "custom" : request.levelTag().trim();
    }

    private void applyNews(NewsArticle article, NewsUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("文章内容不能为空");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("文章标题不能为空");
        }
        article.title = request.title().trim();
        article.summary = trim(request.summary());
        article.content = trim(request.content());
        article.sourceName = trim(request.sourceName());
        article.difficulty = request.difficulty() == null || request.difficulty().isBlank() ? "medium" : request.difficulty().trim();
        article.topicTags = trim(request.topicTags());
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record AdminSummary(
            long users,
            long activeUsers,
            long words,
            long news,
            long knowledgeDocuments,
            long essays,
            long readings,
            long reviewWords,
            long conversations,
            long todayWordReviews,
            long todayUniqueWords,
            long todayLearners,
            List<DailyWordStat> dailyWordStats
    ) {
    }

    public record DailyWordStat(String date, long words) {
    }

    public record AdminUserView(
            Long id,
            String username,
            String nickname,
            String role,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        static AdminUserView from(UserAccount user) {
            return new AdminUserView(user.id, user.username, user.nickname, user.role, user.status, user.createdAt, user.updatedAt);
        }
    }

    public record UserStatusRequest(String status) {
    }

    public record PasswordResetRequest(String password) {
    }

    public record UserUpdateRequest(
            String nickname,
            String status,
            String password
    ) {
    }

    public record AdminUserProfile(
            Long id,
            String username,
            String nickname,
            String role,
            String status,
            int level,
            int experience,
            int totalExperience,
            long favoriteWords,
            long learnedWords,
            long masteredWords,
            long reviewWords,
            long dueWords,
            long reviewLogs,
            long wrongReviewLogs,
            long essays,
            long readings,
            long finishedReadings,
            long lookedUpWords,
            long conversations,
            VocabularyDistribution vocabularyDistribution,
            ReadingOverview readingOverview,
            boolean aiConfigured
    ) {
    }

    public record VocabularyDistribution(
            long learnedWords,
            long masteredWords,
            long reviewWords,
            long dueWords,
            long favoriteWords
    ) {
    }

    public record ReadingOverview(
            long readings,
            long finishedReadings,
            long lookedUpWords
    ) {
    }

    public record WordUpsertRequest(
            String word,
            String phoneticUs,
            String phoneticUk,
            String meaningCn,
            String partOfSpeech,
            String exampleEn,
            String exampleCn,
            String levelTag
    ) {
    }

    public record AdminWordPage(List<Word> words, long total, int page, int size) {
    }

    public record AdminNewsPage(List<NewsArticle> articles, long total, int page, int size) {
    }

    public record NewsUpsertRequest(
            String title,
            String summary,
            String content,
            String sourceName,
            String difficulty,
            String topicTags
    ) {
    }

    public record AdminReadingRecordView(
            Long id,
            Long articleId,
            String title,
            String difficulty,
            Integer readProgress,
            Integer lookedUpWords,
            Boolean finished,
            LocalDateTime createdAt
    ) {
        static AdminReadingRecordView from(ReadingRecord record, NewsArticle article) {
            return new AdminReadingRecordView(
                    record.id,
                    record.articleId,
                    article == null ? "文章已删除" : article.title,
                    article == null ? "" : article.difficulty,
                    record.readProgress,
                    record.lookedUpWords,
                    record.finished,
                    record.createdAt
            );
        }
    }
}
