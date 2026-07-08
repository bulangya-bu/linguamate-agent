package com.example.javabigwork.news.dto;

import java.time.LocalDateTime;

public record ReadingProgressView(
        Long articleId,
        Integer readProgress,
        Integer lookedUpWords,
        Boolean finished,
        LocalDateTime updatedAt
) {
}
