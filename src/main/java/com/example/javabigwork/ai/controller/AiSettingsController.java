package com.example.javabigwork.ai.controller;

import com.example.javabigwork.ai.dto.AiConnectionRequest;
import com.example.javabigwork.ai.dto.AiModelOption;
import com.example.javabigwork.ai.dto.AiSettingsRequest;
import com.example.javabigwork.ai.dto.AiSettingsView;
import com.example.javabigwork.ai.service.AiSettingsService;
import com.example.javabigwork.ai.service.OpenAiCompatibleClient;
import com.example.javabigwork.common.ApiResponse;
import java.util.List;
import java.util.Map;
import com.example.javabigwork.ai.dto.OpenAiChatMessage;
import org.springframework.web.client.RestClientResponseException;
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

    @PostMapping("/test")
    public ApiResponse<Map<String, Object>> test(@RequestBody(required = false) AiConnectionRequest request) {
        var settings = settingsService.resolveConnection(request);
        if (settings.chatModel() == null || settings.chatModel().isBlank()) {
            throw new IllegalArgumentException("请先填写模型名");
        }
        long start = System.nanoTime();
        try {
            client.chat(settings, "Reply with OK only.", List.of(new OpenAiChatMessage("user", "Connection test.")));
            return ApiResponse.ok(Map.of("available", true, "modelName", settings.chatModel(),
                    "latencyMs", (System.nanoTime() - start) / 1_000_000));
        } catch (RestClientResponseException exception) {
            throw new IllegalArgumentException("模型连接失败（HTTP " + exception.getStatusCode().value() + "），请检查地址、密钥和模型名");
        } catch (Exception exception) {
            throw new IllegalArgumentException("模型暂时无法响应，请检查配置或稍后重试");
        }
    }
}
