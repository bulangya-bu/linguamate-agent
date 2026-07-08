package com.example.javabigwork.news.dto;

public record ReadingProgressRequest(
        Integer readProgress,
        Boolean finished
) {
}
