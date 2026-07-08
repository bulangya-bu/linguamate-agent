package com.example.javabigwork.vocabulary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WordReviewRequest(
        @NotBlank(message = "请输入英文拼写")
        @Size(max = 255, message = "英文拼写不能超过 255 个字符")
        String spellingAnswer,

        @NotBlank(message = "请输入中文释义")
        @Size(max = 255, message = "中文释义不能超过 255 个字符")
        String meaningAnswer
) {
}
