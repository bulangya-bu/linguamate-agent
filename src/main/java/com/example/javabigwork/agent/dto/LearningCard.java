package com.example.javabigwork.agent.dto;

import java.util.List;

public record LearningCard(
        String type,
        String title,
        String summary,
        String group,
        List<LearningCardItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
