package com.example.javabigwork.knowledge.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.knowledge.dto.KnowledgeSearchResult;
import com.example.javabigwork.knowledge.entity.KnowledgeChunk;
import com.example.javabigwork.knowledge.entity.KnowledgeDocument;
import com.example.javabigwork.knowledge.mapper.KnowledgeChunkMapper;
import com.example.javabigwork.knowledge.mapper.KnowledgeDocumentMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RetrievalService {
    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);
    private final KnowledgeChunkMapper chunkMapper;
    private final KnowledgeDocumentMapper documentMapper;

    public RetrievalService(KnowledgeChunkMapper chunkMapper, KnowledgeDocumentMapper documentMapper) {
        this.chunkMapper = chunkMapper;
        this.documentMapper = documentMapper;
    }

    public List<KnowledgeSearchResult> search(String query, int limit) {
        String keyword = query == null || query.isBlank() ? "" : query.trim();
        log.info("RetrievalService.search: query={}, limit={}", keyword, limit);
        List<KnowledgeChunk> chunks = chunkMapper.selectList(Wrappers.<KnowledgeChunk>query()
                .like(!keyword.isBlank(), "content", keyword)
                .or(!keyword.isBlank())
                .like(!keyword.isBlank(), "keywords", keyword)
                .orderByDesc("created_at")
                .last("LIMIT " + Math.max(1, limit)));
        return chunks.stream()
                .map(chunk -> {
                    KnowledgeDocument document = documentMapper.selectById(chunk.documentId);
                    String title = document == null ? "Unknown" : document.title;
                    String sourceType = document == null ? "unknown" : document.sourceType;
                    String tags = document == null ? "" : document.tags;
                    return new KnowledgeSearchResult(chunk.id, title, chunk.content, sourceType, tags);
                })
                .toList();
    }
}
