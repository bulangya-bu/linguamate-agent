package com.example.javabigwork.ai.service;

import com.example.javabigwork.ai.dto.AiRuntimeSettings;
import com.example.javabigwork.ai.dto.AiModelOption;
import com.example.javabigwork.ai.dto.OpenAiChatCompletionRequest;
import com.example.javabigwork.ai.dto.OpenAiChatCompletionResponse;
import com.example.javabigwork.ai.dto.OpenAiChatMessage;
import com.example.javabigwork.ai.dto.OpenAiModelListResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class OpenAiCompatibleClient {

    public List<AiModelOption> listModels(AiRuntimeSettings settings) {
        OpenAiModelListResponse response = restClient(settings)
                .get()
                .uri("/models")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(OpenAiModelListResponse.class);
        if (response == null || response.data() == null) {
            return List.of();
        }
        return response.data().stream()
                .map(OpenAiModelListResponse.ModelItem::id)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .map(AiModelOption::new)
                .toList();
    }

    public String chat(AiRuntimeSettings settings, String systemPrompt, List<OpenAiChatMessage> history) {
        List<OpenAiChatMessage> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(new OpenAiChatMessage("system", systemPrompt));
        }
        if (history != null) {
            messages.addAll(history);
        }
        OpenAiChatCompletionResponse response = restClient(settings)
                .post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new OpenAiChatCompletionRequest(settings.chatModel(), messages, false))
                .retrieve()
                .body(OpenAiChatCompletionResponse.class);
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalArgumentException("模型返回为空");
        }
        OpenAiChatCompletionResponse.Choice choice = response.choices().get(0);
        if (choice == null || choice.message() == null || !StringUtils.hasText(choice.message().content())) {
            throw new IllegalArgumentException("模型返回内容为空");
        }
        return choice.message().content().trim();
    }

    private RestClient restClient(AiRuntimeSettings settings) {
        String baseUrl = normalizeBaseUrl(settings.baseUrl());
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        if (StringUtils.hasText(settings.apiKey())) {
            builder.defaultHeader("Authorization", "Bearer " + settings.apiKey().trim());
            builder.defaultHeader("api-key", settings.apiKey().trim());
        }
        return builder.build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalArgumentException("模型地址不能为空");
        }
        String trimmed = baseUrl.trim().replaceAll("/+$", "");
        if (trimmed.matches(".*?/v\\d+$")) {
            return trimmed;
        }
        if (trimmed.contains("://") && !trimmed.endsWith("/v1")) {
            return trimmed + "/v1";
        }
        return trimmed;
    }
}
