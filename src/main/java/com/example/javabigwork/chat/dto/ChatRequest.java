package com.example.javabigwork.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record ChatRequest(
        Long conversationId,
        String scene,
        @NotBlank(message = "请输入想练习或咨询的内容")
        @Size(max = 2000, message = "消息不能超过 2000 个字符")
        String message,
        @Pattern(regexp = "practice|assistant", message = "请选择有效的对话模式")
        String mode,
        Long readingArticleId
) {
    public ChatRequest(Long conversationId, String scene, String message, String mode) {
        this(conversationId, scene, message, mode, null);
    }
}
