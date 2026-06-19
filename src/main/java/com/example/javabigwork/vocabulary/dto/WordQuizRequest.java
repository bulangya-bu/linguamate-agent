package com.example.javabigwork.vocabulary.dto;

import jakarta.validation.constraints.NotBlank;

public record WordQuizRequest(@NotBlank(message = "请输入答案") String answer) {
}
