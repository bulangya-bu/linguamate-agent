package com.example.javabigwork.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        Long conversationId,
        String scene,
        @NotBlank(message = "请输入想练习或咨询的内容") String message
) {
}
