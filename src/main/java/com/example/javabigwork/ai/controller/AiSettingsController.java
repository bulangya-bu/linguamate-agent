package com.example.javabigwork.ai.controller;

import com.example.javabigwork.ai.dto.AiConnectionRequest;
import com.example.javabigwork.ai.dto.AiModelOption;
import com.example.javabigwork.ai.dto.AiSettingsRequest;
import com.example.javabigwork.ai.dto.AiSettingsView;
import com.example.javabigwork.ai.service.AiSettingsService;
import com.example.javabigwork.ai.service.OpenAiCompatibleClient;
import com.example.javabigwork.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiSettingsController {
    private final AiSettingsService settingsService;
    private final OpenAiCompatibleClient client;

    public AiSettingsController(AiSettingsService settingsService, OpenAiCompatibleClient client) {
        this.settingsService = settingsService;
        this.client = client;
    }

    @GetMapping("/settings")
    public ApiResponse<AiSettingsView> settings() {
        return ApiResponse.ok(settingsService.currentView());
    }

    @PutMapping("/settings")
    public ApiResponse<AiSettingsView> save(@RequestBody AiSettingsRequest request) {
        return ApiResponse.ok(settingsService.save(request));
    }

    @PostMapping("/models")
    public ApiResponse<List<AiModelOption>> models(@RequestBody(required = false) AiConnectionRequest request) {
        return ApiResponse.ok(client.listModels(settingsService.resolveConnection(request)));
    }
}
