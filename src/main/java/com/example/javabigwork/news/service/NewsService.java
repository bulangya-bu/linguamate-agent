package com.example.javabigwork.news.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.cache.RedisCacheService;
import com.example.javabigwork.news.dto.NewsArticleView;
import com.example.javabigwork.news.dto.ReadingProgressRequest;
import com.example.javabigwork.news.dto.ReadingProgressView;
import com.example.javabigwork.news.entity.NewsArticle;
import com.example.javabigwork.news.entity.ReadingRecord;
import com.example.javabigwork.news.mapper.NewsArticleMapper;
import com.example.javabigwork.news.mapper.ReadingRecordMapper;
import com.example.javabigwork.tool.NewsRecommendTool;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NewsService {
    private static final int FINISHED_THRESHOLD = 90;
    private static final Duration RECOMMENDATION_TTL = Duration.ofMinutes(10);

    private final NewsRecommendTool recommendTool;
    private final NewsArticleMapper articleMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final UserContext userContext;
    private final RedisCacheService redisCache;

    public NewsService(
            NewsRecommendTool recommendTool,
            NewsArticleMapper articleMapper,
            ReadingRecordMapper readingRecordMapper,
            UserContext userContext,
            RedisCacheService redisCache
    ) {
        this.recommendTool = recommendTool;
        this.articleMapper = articleMapper;
        this.readingRecordMapper = readingRecordMapper;
        this.userContext = userContext;
        this.redisCache = redisCache;
    }

    public List<NewsArticleView> recommendations(String difficulty, String topic) {
        String safeDifficulty = difficulty == null ? "" : difficulty.trim();
        String safeTopic = topic == null ? "" : topic.trim();
        Long userId = userContext.userId();
        String cacheKey = recommendationKey(userId, safeDifficulty, safeTopic);
        List<NewsArticleView> cached = redisCache.getList(cacheKey, NewsArticleView.class).orElse(null);
        if (cached != null) {
            return cached;
        }
        Map<Long, ReadingRecord> records = readingRecordMap(userId);
        Map<String, Integer> topicWeights = StringUtils.hasText(safeTopic) ? Map.of() : topicWeights(userId);
        List<NewsArticleView> recommendations = recommendTool.recommend(safeDifficulty, safeTopic, 100).stream()
                .sorted((left, right) -> compareRecommendations(left, right, records, topicWeights))
                .limit(10)
                .map(article -> toView(article, records.get(article.id)))
                .toList();
        redisCache.putIndexed(recommendationIndexKey(), cacheKey, recommendations, RECOMMENDATION_TTL);
        redisCache.index(userRecommendationIndexKey(userId), cacheKey, RECOMMENDATION_TTL);
        return recommendations;
    }

    public List<String> topics() {
        Set<String> topics = new LinkedHashSet<>();
        articleMapper.selectList(Wrappers.<NewsArticle>query()
                        .orderByAsc("topic_tags")
                        .orderByDesc("created_at")
                        .orderByDesc("id"))
                .forEach(article -> splitTopics(article.topicTags).forEach(topics::add));
        return new ArrayList<>(topics);
    }

    @Transactional
    public NewsArticleView detail(Long id) {
        NewsArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new IllegalArgumentException("News article does not exist.");
        }
        ReadingRecord record = ensureReadingRecord(article.id);
        evictUserRecommendationCache(record.userId);
        return toView(article, record);
    }

    @Transactional
    public ReadingProgressView updateProgress(Long articleId, ReadingProgressRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Reading progress is required.");
        }
        NewsArticle article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("News article does not exist.");
        }
        int requestedProgress = request.readProgress() == null ? 0 : request.readProgress();
        if (requestedProgress < 0 || requestedProgress > 100) {
            throw new IllegalArgumentException("Reading progress must be between 0 and 100.");
        }

        ReadingRecord record = ensureReadingRecord(articleId);
        int nextProgress = Math.max(value(record.readProgress), requestedProgress);
        boolean manuallyFinished = Boolean.TRUE.equals(request.finished());
        if (manuallyFinished) {
            nextProgress = 100;
        }
        record.readProgress = Math.min(100, nextProgress);
        record.finished = Boolean.TRUE.equals(record.finished)
                || manuallyFinished
                || record.readProgress >= FINISHED_THRESHOLD;
        record.updatedAt = LocalDateTime.now();
        readingRecordMapper.updateById(record);
        evictUserRecommendationCache(record.userId);
        return toProgressView(record);
    }

    @Transactional
    public void recordLookup(Long articleId) {
        NewsArticle article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("News article does not exist.");
        }
        ReadingRecord record = ensureReadingRecord(articleId);
        record.lookedUpWords = value(record.lookedUpWords) + 1;
        record.updatedAt = LocalDateTime.now();
        readingRecordMapper.updateById(record);
        evictUserRecommendationCache(record.userId);
    }

    private String recommendationKey(Long userId, String difficulty, String topic) {
        return redisCache.key("news:recommendations", userId, difficulty, topic);
    }

    private String recommendationIndexKey() {
        return redisCache.key("news:recommendations:index");
    }

    private String userRecommendationIndexKey(Long userId) {
        return redisCache.key("news:recommendations:user", userId, "index");
    }

    private void evictUserRecommendationCache(Long userId) {
        if (userId != null) {
            redisCache.evictIndexed(userRecommendationIndexKey(userId));
        }
    }

    private ReadingRecord ensureReadingRecord(Long articleId) {
        Long userId = userContext.userId();
        ReadingRecord record = findReadingRecord(userId, articleId);
        if (record != null) {
            if (record.updatedAt == null) {
                record.updatedAt = record.createdAt == null ? LocalDateTime.now() : record.createdAt;
                readingRecordMapper.updateById(record);
            }
            return record;
        }

        LocalDateTime now = LocalDateTime.now();
        record = new ReadingRecord();
        record.userId = userId;
        record.articleId = articleId;
        record.readProgress = 0;
        record.lookedUpWords = 0;
        record.finished = false;
        record.createdAt = now;
        record.updatedAt = now;
        readingRecordMapper.insert(record);
        return record;
    }

    private ReadingRecord findReadingRecord(Long userId, Long articleId) {
        return readingRecordMapper.selectList(Wrappers.<ReadingRecord>query()
                        .eq("user_id", userId)
                        .eq("article_id", articleId)
                        .orderByDesc("updated_at")
                        .orderByDesc("id")
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private int compareRecommendations(
            NewsArticle left,
            NewsArticle right,
            Map<Long, ReadingRecord> records,
            Map<String, Integer> topicWeights
    ) {
        int statusCompare = Integer.compare(
                recommendationRank(records.get(right.id)),
                recommendationRank(records.get(left.id))
        );
        if (statusCompare != 0) {
            return statusCompare;
        }

        int topicCompare = Integer.compare(topicScore(right, topicWeights), topicScore(left, topicWeights));
        if (topicCompare != 0) {
            return topicCompare;
        }

        if (left.createdAt == null && right.createdAt == null) {
            return Long.compare(value(right.id), value(left.id));
        }
        if (left.createdAt == null) {
            return 1;
        }
        if (right.createdAt == null) {
            return -1;
        }
        int createdCompare = right.createdAt.compareTo(left.createdAt);
        return createdCompare != 0 ? createdCompare : Long.compare(value(right.id), value(left.id));
    }

    private int recommendationRank(ReadingRecord record) {
        if (record == null) {
            return 2;
        }
        return Boolean.TRUE.equals(record.finished) ? 1 : 3;
    }

    private int topicScore(NewsArticle article, Map<String, Integer> topicWeights) {
        if (topicWeights.isEmpty()) {
            return 0;
        }
        return splitTopics(article.topicTags).stream()
                .map(topic -> topic.toLowerCase(Locale.ROOT))
                .mapToInt(topic -> topicWeights.getOrDefault(topic, 0))
                .sum();
    }

    private Map<String, Integer> topicWeights(Long userId) {
        Map<String, Integer> weights = new HashMap<>();
        readingRecordMapper.selectList(Wrappers.<ReadingRecord>query()
                        .eq("user_id", userId)
                        .orderByDesc("updated_at")
                        .orderByDesc("id")
                        .last("LIMIT 50"))
                .forEach(record -> {
                    if (record.articleId == null) {
                        return;
                    }
                    NewsArticle article = articleMapper.selectById(record.articleId);
                    if (article == null) {
                        return;
                    }
                    int weight = 1 + value(record.lookedUpWords) + Math.max(0, value(record.readProgress) / 25);
                    if (!Boolean.TRUE.equals(record.finished)) {
                        weight += 2;
                    }
                    int finalWeight = weight;
                    splitTopics(article.topicTags).forEach(topic ->
                            weights.merge(topic.toLowerCase(Locale.ROOT), finalWeight, Integer::sum));
                });
        return weights;
    }

    private Map<Long, ReadingRecord> readingRecordMap(Long userId) {
        Map<Long, ReadingRecord> records = new HashMap<>();
        readingRecordMapper.selectList(Wrappers.<ReadingRecord>query().eq("user_id", userId))
                .forEach(record -> {
                    if (record.articleId == null) {
                        return;
                    }
                    records.merge(record.articleId, record, this::newerRecord);
                });
        return records;
    }

    private ReadingRecord newerRecord(ReadingRecord left, ReadingRecord right) {
        LocalDateTime leftTime = left.updatedAt == null ? left.createdAt : left.updatedAt;
        LocalDateTime rightTime = right.updatedAt == null ? right.createdAt : right.updatedAt;
        if (leftTime == null) {
            return right;
        }
        if (rightTime == null) {
            return left;
        }
        return rightTime.isAfter(leftTime) ? right : left;
    }

    private List<String> splitTopics(String topicTags) {
        if (!StringUtils.hasText(topicTags)) {
            return List.of();
        }
        return Arrays.stream(topicTags.split("[,，;；\\s]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private NewsArticleView toView(NewsArticle article, ReadingRecord record) {
        return new NewsArticleView(
                article.id,
                article.title,
                article.summary,
                article.content,
                article.sourceName,
                article.difficulty,
                article.topicTags,
                record == null ? 0 : value(record.readProgress),
                record != null && Boolean.TRUE.equals(record.finished)
        );
    }

    private ReadingProgressView toProgressView(ReadingRecord record) {
        return new ReadingProgressView(
                record.articleId,
                value(record.readProgress),
                value(record.lookedUpWords),
                Boolean.TRUE.equals(record.finished),
                record.updatedAt == null ? record.createdAt : record.updatedAt
        );
    }
}
