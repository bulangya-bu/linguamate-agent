package com.example.javabigwork.agent.dto;

import java.util.Map;

public record ChatAction(
        String id,
        String label,
        String section,
        String route,
        Map<String, String> query) {
}
