package com.example.javabigwork.news.service;

import com.example.javabigwork.ai.dto.AiRuntimeSettings;
import com.example.javabigwork.ai.dto.OpenAiChatMessage;
import com.example.javabigwork.ai.service.AiSettingsService;
import com.example.javabigwork.ai.service.OpenAiCompatibleClient;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NewsTranslationService {
    private final AiSettingsService settingsService;
    private final OpenAiCompatibleClient aiClient;

    public NewsTranslationService(
            AiSettingsService settingsService,
            OpenAiCompatibleClient aiClient
    ) {
        this.settingsService = settingsService;
        this.aiClient = aiClient;
    }

    public String translate(String text) {
        if (!StringUtils.hasText(text)) return "";
        AiRuntimeSettings settings = settingsService.resolveCurrent();
        if (!StringUtils.hasText(settings.baseUrl()) || !StringUtils.hasText(settings.apiKey())) {
            return "";
        }
        try {
            String prompt = "Translate the following English text to Simplified Chinese. Return ONLY the Chinese translation, nothing else:\n\n" + text;
            List<OpenAiChatMessage> messages = new ArrayList<>();
            messages.add(new OpenAiChatMessage("user", prompt));
            String result = aiClient.chat(settings, "You are a translator. Translate English to Chinese concisely.", messages);
            return result != null ? result.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
