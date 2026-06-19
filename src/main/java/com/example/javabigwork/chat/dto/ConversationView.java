package com.example.javabigwork.chat.dto;

import java.time.LocalDateTime;

public record ConversationView(Long id, String scene, String title, LocalDateTime updatedAt) {
}
