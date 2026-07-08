package com.example.javabigwork.vocabulary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WordQuizRequest(
        @NotBlank(message = "请输入答案")
        @Size(max = 255, message = "答案不能超过 255 个字符")
        String answer
) {
}
