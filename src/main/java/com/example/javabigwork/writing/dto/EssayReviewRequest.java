package com.example.javabigwork.writing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EssayReviewRequest(
        @Size(max = 120, message = "标题不能超过 120 个字符")
        String title,
        @Size(max = 64, message = "考试类型不能超过 64 个字符")
        String examType,
        @Size(max = 64, message = "目标等级不能超过 64 个字符")
        String targetLevel,
        @NotBlank(message = "请输入作文内容")
        @Size(max = 8000, message = "作文内容不能超过 8000 个字符")
        String essayText,
        Long revisionOfId
) {
    public EssayReviewRequest(String title, String examType, String targetLevel, String essayText) {
        this(title, examType, targetLevel, essayText, null);
    }
}
