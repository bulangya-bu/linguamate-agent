package com.example.javabigwork.knowledge.controller;

import com.example.javabigwork.common.ApiResponse;
import com.example.javabigwork.knowledge.dto.KnowledgeSearchResult;
import com.example.javabigwork.knowledge.dto.KnowledgeUploadRequest;
import com.example.javabigwork.knowledge.entity.KnowledgeDocument;
import com.example.javabigwork.knowledge.service.KnowledgeService;
import com.example.javabigwork.knowledge.service.RetrievalService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeService knowledgeService;
    private final RetrievalService retrievalService;

    public KnowledgeController(KnowledgeService knowledgeService, RetrievalService retrievalService) {
        this.knowledgeService = knowledgeService;
        this.retrievalService = retrievalService;
    }

    @PostMapping
    public ApiResponse<KnowledgeDocument> upload(@Valid @RequestBody KnowledgeUploadRequest request) {
        return ApiResponse.ok("知识已导入并切分", knowledgeService.upload(request));
    }

    @GetMapping
    public ApiResponse<List<KnowledgeDocument>> list() {
        return ApiResponse.ok(knowledgeService.list());
    }

    @PostMapping("/search")
    public ApiResponse<List<KnowledgeSearchResult>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ApiResponse.ok(retrievalService.search(query, limit));
    }
}
