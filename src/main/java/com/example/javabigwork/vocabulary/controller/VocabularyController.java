package com.example.javabigwork.vocabulary.controller;

import com.example.javabigwork.common.ApiResponse;
import com.example.javabigwork.news.dto.HoverWordResponse;
import com.example.javabigwork.tool.DictionaryTool;
import com.example.javabigwork.vocabulary.dto.WordAiDetailView;
import com.example.javabigwork.vocabulary.dto.WordCardView;
import com.example.javabigwork.vocabulary.dto.WordQuizRequest;
import com.example.javabigwork.vocabulary.dto.WordQuizResult;
import com.example.javabigwork.vocabulary.dto.WordReviewRequest;
import com.example.javabigwork.vocabulary.service.ReviewScheduleService;
import com.example.javabigwork.vocabulary.service.WordAiDetailService;
import com.example.javabigwork.vocabulary.service.VocabularyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class VocabularyController {
    private final VocabularyService vocabularyService;
    private final ReviewScheduleService reviewScheduleService;
    private final WordAiDetailService wordAiDetailService;
    private final DictionaryTool dictionaryTool;

    public VocabularyController(
            VocabularyService vocabularyService,
            ReviewScheduleService reviewScheduleService,
            WordAiDetailService wordAiDetailService,
            DictionaryTool dictionaryTool
    ) {
        this.vocabularyService = vocabularyService;
        this.reviewScheduleService = reviewScheduleService;
        this.wordAiDetailService = wordAiDetailService;
        this.dictionaryTool = dictionaryTool;
    }

    @GetMapping("/words/today")
    public ApiResponse<List<WordCardView>> todayWords(
            @RequestParam(defaultValue = "8") int limit,
            @RequestParam(required = false) String category
    ) {
        return ApiResponse.ok(vocabularyService.todayWords(limit, category));
    }

    @PostMapping("/words/{id}/quiz")
    public ApiResponse<WordQuizResult> quiz(@PathVariable Long id, @Valid @RequestBody WordQuizRequest request) {
        return ApiResponse.ok(vocabularyService.submitQuiz(id, request));
    }

    @GetMapping("/words/{id}/ai-detail")
    public ApiResponse<WordAiDetailView> aiDetail(@PathVariable Long id) {
        return ApiResponse.ok(wordAiDetailService.detail(id));
    }

    @PostMapping("/words/{id}/favorite")
    public ApiResponse<WordCardView> favorite(@PathVariable Long id) {
        return ApiResponse.ok(vocabularyService.toggleFavorite(id));
    }

    @GetMapping("/reviews/today")
    public ApiResponse<List<WordCardView>> todayReviews(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok(reviewScheduleService.todayReviewWords(limit));
    }

    @GetMapping("/reviews/favorites")
    public ApiResponse<List<WordCardView>> favoriteReviews(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(reviewScheduleService.favoriteWords(limit));
    }

    @GetMapping("/reviews/mastered")
    public ApiResponse<List<WordCardView>> masteredReviews(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(reviewScheduleService.masteredWords(limit));
    }

    @PostMapping("/reviews/{wordId}")
    public ApiResponse<WordQuizResult> review(@PathVariable Long wordId, @Valid @RequestBody WordReviewRequest request) {
        return ApiResponse.ok(vocabularyService.submitStrictReview(wordId, request));
    }

    @GetMapping("/dictionary/lookup")
    public ApiResponse<HoverWordResponse> lookup(@RequestParam String word) {
        return ApiResponse.ok(dictionaryTool.lookup(word));
    }

    @PostMapping("/dictionary/save")
    public ApiResponse<HoverWordResponse> save(@RequestParam String word) {
        return ApiResponse.ok("已加入错词本", dictionaryTool.saveToReviewBook(word));
    }
}
