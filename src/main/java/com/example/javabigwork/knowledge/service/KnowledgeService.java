package com.example.javabigwork.knowledge.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.knowledge.dto.KnowledgeUploadRequest;
import com.example.javabigwork.knowledge.entity.KnowledgeChunk;
import com.example.javabigwork.knowledge.entity.KnowledgeDocument;
import com.example.javabigwork.knowledge.mapper.KnowledgeChunkMapper;
import com.example.javabigwork.knowledge.mapper.KnowledgeDocumentMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeService {
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final DocumentChunkService chunkService;

    public KnowledgeService(
            KnowledgeDocumentMapper documentMapper,
            KnowledgeChunkMapper chunkMapper,
            DocumentChunkService chunkService
    ) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.chunkService = chunkService;
    }

    public KnowledgeDocument upload(KnowledgeUploadRequest request) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.title = request.title();
        document.sourceType = request.sourceType() == null || request.sourceType().isBlank() ? "manual" : request.sourceType();
        document.tags = request.tags();
        document.content = request.content();
        document.createdAt = LocalDateTime.now();
        documentMapper.insert(document);

        List<String> chunks = chunkService.split(request.content());
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.documentId = document.id;
            chunk.chunkIndex = i;
            chunk.content = chunks.get(i);
            chunk.keywords = request.tags();
            chunk.embeddingRef = "demo-keyword-index";
            chunk.createdAt = LocalDateTime.now();
            chunkMapper.insert(chunk);
        }
        return document;
    }

    public List<KnowledgeDocument> list() {
        return documentMapper.selectList(Wrappers.<KnowledgeDocument>query().orderByDesc("created_at"));
    }
}
