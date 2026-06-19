package com.example.javabigwork.ai.dto;

import java.util.List;

public record OpenAiModelListResponse(
        List<ModelItem> data
) {
    public record ModelItem(String id) {
    }
}
