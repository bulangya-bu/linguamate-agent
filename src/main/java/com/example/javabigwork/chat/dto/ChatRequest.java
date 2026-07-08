package com.example.javabigwork.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        Long conversationId,
        String scene,
        @NotBlank(message = "请输入想练习或咨询的内容")
        @Size(max = 2000, message = "消息不能超过 2000 个字符")
        String message
) {
}
