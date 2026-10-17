package com.example.javabigwork.agent.controller;

import com.example.javabigwork.agent.dto.LearningAdvisorView;
import com.example.javabigwork.agent.service.LearningAdvisorService;
import com.example.javabigwork.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning-advisor")
public class LearningAdvisorController {

    private final LearningAdvisorService learningAdvisorService;

    public LearningAdvisorController(LearningAdvisorService learningAdvisorService) {
        this.learningAdvisorService = learningAdvisorService;
    }

    @GetMapping("/vocabulary")
    public ApiResponse<LearningAdvisorView> vocabulary(
            @RequestParam String group,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(learningAdvisorService.vocabularySummary(group, page, size));
    }
}
