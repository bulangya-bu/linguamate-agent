package com.example.javabigwork.news.dto;

public record GNewsArticleView(
        String title,
        String titleCn,
        String description,
        String descriptionCn,
        String content,
        String url,
        String sourceName,
        String publishedAt
) {
}
