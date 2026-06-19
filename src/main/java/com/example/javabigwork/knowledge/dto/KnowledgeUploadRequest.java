package com.example.javabigwork.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeUploadRequest(
        @NotBlank(message = "请输入知识标题") String title,
        String sourceType,
        String tags,
        @NotBlank(message = "请输入知识内容") String content
) {
}
