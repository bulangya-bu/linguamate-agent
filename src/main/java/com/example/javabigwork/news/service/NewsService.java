package com.example.javabigwork.news.service;

import com.example.javabigwork.news.dto.NewsArticleView;
import com.example.javabigwork.news.entity.NewsArticle;
import com.example.javabigwork.news.mapper.NewsArticleMapper;
import com.example.javabigwork.tool.NewsRecommendTool;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NewsService {
    private final NewsRecommendTool recommendTool;
    private final NewsArticleMapper articleMapper;

    public NewsService(NewsRecommendTool recommendTool, NewsArticleMapper articleMapper) {
        this.recommendTool = recommendTool;
        this.articleMapper = articleMapper;
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
        return toView(article);
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
