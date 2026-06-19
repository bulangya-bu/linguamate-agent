package com.example.javabigwork.writing.dto;

import jakarta.validation.constraints.NotBlank;

public record EssayReviewRequest(
        String title,
        String examType,
        String targetLevel,
        @NotBlank(message = "请输入作文内容") String essayText
) {
}
