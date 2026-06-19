package com.example.javabigwork.news.dto;

public record NewsArticleView(
        Long id,
        String title,
        String summary,
        String content,
        String sourceName,
        String difficulty,
        String topicTags
) {
}
