package com.example.javabigwork.vocabulary.controller;

import com.example.javabigwork.common.ApiResponse;
import com.example.javabigwork.vocabulary.dto.WordBankCategory;
import com.example.javabigwork.vocabulary.dto.WordBankPage;
import com.example.javabigwork.vocabulary.dto.WordBankStats;
import com.example.javabigwork.vocabulary.service.WordBankService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/word-bank")
public class WordBankController {
    private final WordBankService wordBankService;

    public WordBankController(WordBankService wordBankService) {
        this.wordBankService = wordBankService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<WordBankCategory>> categories() {
        return ApiResponse.ok(wordBankService.categories());
    }

    @GetMapping("/words")
    public ApiResponse<WordBankPage> words(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "all") String scope
    ) {
        return ApiResponse.ok(wordBankService.words(category, search, page, size, scope));
    }

    @GetMapping("/stats")
    public ApiResponse<WordBankStats> stats() {
        return ApiResponse.ok(wordBankService.stats());
    }
}
