package com.example.javabigwork.news.controller;

import com.example.javabigwork.common.ApiResponse;
import com.example.javabigwork.news.dto.GNewsArticleView;
import com.example.javabigwork.news.dto.HoverWordResponse;
import com.example.javabigwork.news.dto.NewsArticleView;
import com.example.javabigwork.news.service.ArticleFetchService;
import com.example.javabigwork.news.service.GNewsService;
import com.example.javabigwork.news.service.NewsService;
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
    private final GNewsService gnewsService;
    private final ArticleFetchService articleFetchService;

    public NewsController(NewsService newsService, ReadingAssistService readingAssistService, GNewsService gnewsService, ArticleFetchService articleFetchService) {
        this.newsService = newsService;
        this.readingAssistService = readingAssistService;
        this.gnewsService = gnewsService;
        this.articleFetchService = articleFetchService;
    }

    @GetMapping("/recommendations")
    public ApiResponse<List<NewsArticleView>> recommendations(
            @RequestParam(defaultValue = "") String difficulty,
            @RequestParam(defaultValue = "") String topic
    ) {
        return ApiResponse.ok(newsService.recommendations(difficulty, topic));
    }

    @GetMapping("/{id}")
    public ApiResponse<NewsArticleView> detail(@PathVariable Long id) {
        return ApiResponse.ok(newsService.detail(id));
    }

    @GetMapping("/gsearch")
    public ApiResponse<List<GNewsArticleView>> gsearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "3") int max
    ) {
        return ApiResponse.ok(gnewsService.search(q, max));
    }

    @GetMapping("/lookup")
    public ApiResponse<HoverWordResponse> lookup(@RequestParam String word) {
        return ApiResponse.ok(readingAssistService.lookup(word));
    }

    @PostMapping("/translate")
    public ApiResponse<Map<String, String>> translate(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "");
        String content = body.getOrDefault("content", "");
        String titleCn = gnewsService.translate(title);
        String contentCn = gnewsService.translate(content);
        return ApiResponse.ok(Map.of("titleCn", titleCn, "contentCn", contentCn));
    }

    @PostMapping("/fetch-content")
    public ApiResponse<Map<String, String>> fetchContent(@RequestBody Map<String, String> body) {
        String url = body.getOrDefault("url", "");
        String content = articleFetchService.fetchContent(url);
        return ApiResponse.ok(Map.of("content", content));
    }
}
