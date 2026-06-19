package com.example.javabigwork.writing.controller;

import com.example.javabigwork.common.ApiResponse;
import com.example.javabigwork.writing.dto.EssayReviewRequest;
import com.example.javabigwork.writing.dto.EssayReviewResponse;
import com.example.javabigwork.writing.entity.EssaySubmission;
import com.example.javabigwork.writing.service.WritingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/writing")
public class WritingController {
    private final WritingService writingService;

    public WritingController(WritingService writingService) {
        this.writingService = writingService;
    }

    @PostMapping("/review")
    public ApiResponse<EssayReviewResponse> review(@Valid @RequestBody EssayReviewRequest request) {
        return ApiResponse.ok(writingService.review(request));
    }

    @GetMapping("/history")
    public ApiResponse<List<EssaySubmission>> history() {
        return ApiResponse.ok(writingService.history());
    }
}
