package com.example.javabigwork.news.controller;

import com.example.javabigwork.common.ApiResponse;
import com.example.javabigwork.news.dto.HoverWordResponse;
import com.example.javabigwork.news.dto.NewsArticleView;
import com.example.javabigwork.news.dto.ReadingProgressRequest;
import com.example.javabigwork.news.dto.ReadingProgressView;
import com.example.javabigwork.news.service.NewsService;
import com.example.javabigwork.news.service.NewsTranslationService;
import com.example.javabigwork.news.service.ReadingAssistService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
public class NewsController {
    private final NewsService newsService;
    private final ReadingAssistService readingAssistService;
    private final NewsTranslationService translationService;

    public NewsController(
            NewsService newsService,
            ReadingAssistService readingAssistService,
            NewsTranslationService translationService
    ) {
        this.newsService = newsService;
        this.readingAssistService = readingAssistService;
        this.translationService = translationService;
    }

    @GetMapping("/recommendations")
    public ApiResponse<List<NewsArticleView>> recommendations(
            @RequestParam(defaultValue = "") String difficulty,
            @RequestParam(defaultValue = "") String topic
    ) {
        return ApiResponse.ok(newsService.recommendations(difficulty, topic));
    }

    @GetMapping("/topics")
    public ApiResponse<List<String>> topics() {
        return ApiResponse.ok(newsService.topics());
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<NewsArticleView> detail(@PathVariable Long id) {
        return ApiResponse.ok(newsService.detail(id));
    }

    @PostMapping("/{articleId:\\d+}/progress")
    public ApiResponse<ReadingProgressView> progress(
            @PathVariable Long articleId,
            @RequestBody ReadingProgressRequest request
    ) {
        return ApiResponse.ok(newsService.updateProgress(articleId, request));
    }

    @GetMapping("/lookup")
    public ApiResponse<HoverWordResponse> lookup(@RequestParam String word) {
        return ApiResponse.ok(readingAssistService.lookup(word));
    }

    @GetMapping("/{articleId:\\d+}/lookup")
    public ApiResponse<HoverWordResponse> articleLookup(@PathVariable Long articleId, @RequestParam String word) {
        HoverWordResponse response = readingAssistService.lookup(word);
        newsService.recordLookup(articleId);
        return ApiResponse.ok(response);
    }

    @PostMapping("/translate")
    public ApiResponse<Map<String, String>> translate(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "");
        String content = body.getOrDefault("content", "");
        if ((title + content).isBlank()) {
            throw new IllegalArgumentException("请输入要翻译的内容");
        }
        if (title.length() > 500 || content.length() > 12000) {
            throw new IllegalArgumentException("翻译内容过长，请缩短后再试");
        }
        String titleCn = translationService.translate(title);
        String contentCn = translationService.translate(content);
        return ApiResponse.ok(Map.of("titleCn", titleCn, "contentCn", contentCn));
    }
}
