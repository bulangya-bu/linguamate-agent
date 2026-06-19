package com.example.javabigwork.knowledge.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DocumentChunkService {

    public List<String> split(String content) {
        List<String> chunks = new ArrayList<>();
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            return chunks;
        }
        int chunkSize = 420;
        for (int start = 0; start < normalized.length(); start += chunkSize) {
            int end = Math.min(normalized.length(), start + chunkSize);
            chunks.add(normalized.substring(start, end));
        }
        return chunks;
    }
}
