package com.example.javabigwork.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.news.entity.NewsArticle;
import com.example.javabigwork.news.mapper.NewsArticleMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NewsRecommendTool {
    private static final Logger log = LoggerFactory.getLogger(NewsRecommendTool.class);
    private final NewsArticleMapper articleMapper;

    public NewsRecommendTool(NewsArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    public List<NewsArticle> recommend(String difficulty, String topic, int limit) {
        log.info("NewsRecommendTool.recommend: difficulty={}, topic={}, limit={}", difficulty, topic, limit);
        var query = Wrappers.<NewsArticle>query();
        if (StringUtils.hasText(difficulty)) {
            query.eq("difficulty", difficulty);
        }
        if (StringUtils.hasText(topic)) {
            query.like("topic_tags", topic);
        }
        query.orderByDesc("created_at").orderByDesc("id").last("LIMIT " + Math.max(1, limit));
        return articleMapper.selectList(query);
    }
}
