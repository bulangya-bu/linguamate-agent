package com.example.javabigwork.vocabulary.controller;

import com.example.javabigwork.common.ApiResponse;
import com.example.javabigwork.vocabulary.service.ExperienceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ExperienceController {
    private final ExperienceService experienceService;

    public ExperienceController(ExperienceService experienceService) {
        this.experienceService = experienceService;
    }

    @GetMapping("/experience")
    public ApiResponse<ExperienceService.LevelResult> getExperience() {
        return ApiResponse.ok(experienceService.getStatus());
    }
}
