package com.example.javabigwork.news.controller;

import com.example.javabigwork.common.ApiResponse;
import com.example.javabigwork.news.dto.HoverWordResponse;
import com.example.javabigwork.news.dto.NewsArticleView;
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

    @GetMapping("/{id:\\d+}")
    public ApiResponse<NewsArticleView> detail(@PathVariable Long id) {
        return ApiResponse.ok(newsService.detail(id));
    }

    @GetMapping("/lookup")
    public ApiResponse<HoverWordResponse> lookup(@RequestParam String word) {
        return ApiResponse.ok(readingAssistService.lookup(word));
    }

    @PostMapping("/translate")
    public ApiResponse<Map<String, String>> translate(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "");
        String content = body.getOrDefault("content", "");
        String titleCn = translationService.translate(title);
        String contentCn = translationService.translate(content);
        return ApiResponse.ok(Map.of("titleCn", titleCn, "contentCn", contentCn));
    }
}
