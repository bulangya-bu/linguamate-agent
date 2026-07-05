package com.example.javabigwork.news.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.news.dto.NewsArticleView;
import com.example.javabigwork.news.entity.NewsArticle;
import com.example.javabigwork.news.entity.ReadingRecord;
import com.example.javabigwork.news.mapper.NewsArticleMapper;
import com.example.javabigwork.news.mapper.ReadingRecordMapper;
import com.example.javabigwork.tool.NewsRecommendTool;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NewsService {
    private final NewsRecommendTool recommendTool;
    private final NewsArticleMapper articleMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final UserContext userContext;

    public NewsService(
            NewsRecommendTool recommendTool,
            NewsArticleMapper articleMapper,
            ReadingRecordMapper readingRecordMapper,
            UserContext userContext
    ) {
        this.recommendTool = recommendTool;
        this.articleMapper = articleMapper;
        this.readingRecordMapper = readingRecordMapper;
        this.userContext = userContext;
    }

    public List<NewsArticleView> recommendations(String difficulty, String topic) {
        return recommendTool.recommend(difficulty, topic, 10).stream()
                .map(this::toView)
                .toList();
    }

    public NewsArticleView detail(Long id) {
        NewsArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new IllegalArgumentException("新闻不存在");
        }
        markRead(article.id);
        return toView(article);
    }

    private void markRead(Long articleId) {
        Long userId = userContext.userId();
        ReadingRecord record = readingRecordMapper.selectList(Wrappers.<ReadingRecord>query()
                        .eq("user_id", userId)
                        .eq("article_id", articleId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
        if (record == null) {
            record = new ReadingRecord();
            record.userId = userId;
            record.articleId = articleId;
            record.readProgress = 100;
            record.lookedUpWords = 0;
            record.finished = true;
            record.createdAt = LocalDateTime.now();
            readingRecordMapper.insert(record);
        } else {
            record.readProgress = 100;
            record.finished = true;
            readingRecordMapper.updateById(record);
        }
    }

    private NewsArticleView toView(NewsArticle article) {
        return new NewsArticleView(
                article.id,
                article.title,
                article.summary,
                article.content,
                article.sourceName,
                article.difficulty,
                article.topicTags
        );
    }
}
