package com.example.javabigwork.ai.service;

import com.example.javabigwork.ai.dto.AiRuntimeSettings;
import com.example.javabigwork.agent.AgentPromptFactory;
import com.example.javabigwork.chat.entity.Message;
import com.example.javabigwork.knowledge.dto.KnowledgeSearchResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WebAnswerService {
    private static final Logger log = LoggerFactory.getLogger(WebAnswerService.class);
    private final AiSettingsService settingsService;
    private final OpenAiCompatibleClient client;
    private final AgentPromptFactory promptFactory;

    public WebAnswerService(
            AiSettingsService settingsService,
            OpenAiCompatibleClient client,
            AgentPromptFactory promptFactory
    ) {
        this.settingsService = settingsService;
        this.client = client;
        this.promptFactory = promptFactory;
    }

    public Optional<String> answer(String scene, String message, List<Message> history) {
        return answer(scene, message, history, List.of());
    }

    public Optional<String> answer(
            String scene,
            String message,
            List<Message> history,
            List<KnowledgeSearchResult> ragSnippets
    ) {
        return answer(scene, message, history, ragSnippets, "{}");
    }

    public Optional<String> answer(
            String scene, String message, List<Message> history,
            List<KnowledgeSearchResult> ragSnippets, String learningEvidence
    ) {
        AiRuntimeSettings settings = settingsService.resolveCurrent();
        if (!StringUtils.hasText(settings.baseUrl()) || !StringUtils.hasText(settings.chatModel())) {
            log.warn("LLM not configured: baseUrl={}, model={}", settings.baseUrl(), settings.chatModel());
            return Optional.empty();
        }
        try {
            List<com.example.javabigwork.ai.dto.OpenAiChatMessage> messages = new ArrayList<>();
            if (history != null) {
                history.stream()
                        .filter(item -> ("user".equals(item.role) || "assistant".equals(item.role))
                                && StringUtils.hasText(item.content))
                        .forEach(item -> messages.add(new com.example.javabigwork.ai.dto.OpenAiChatMessage(item.role, item.content)));
            }
            messages.add(new com.example.javabigwork.ai.dto.OpenAiChatMessage("user", message));
            log.info("Calling LLM: scene={}, model={}, messageLen={}", scene, settings.chatModel(), message.length());
            String reply = client.chat(settings, promptFactory.learningAssistantPrompt(scene, ragSnippets, learningEvidence), messages);
            return Optional.of(reply);
        } catch (Exception exception) {
            log.error("LLM call failed: scene={}, baseUrl={}, model={}, error={}",
                    scene, settings.baseUrl(), settings.chatModel(), exception.getMessage());
            return Optional.empty();
        }
    }
}
