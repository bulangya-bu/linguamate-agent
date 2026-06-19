package com.example.javabigwork.writing.service;

import com.example.javabigwork.ai.dto.AiRuntimeSettings;
import com.example.javabigwork.ai.dto.OpenAiChatMessage;
import com.example.javabigwork.ai.service.AiSettingsService;
import com.example.javabigwork.ai.service.OpenAiCompatibleClient;
import com.example.javabigwork.agent.AgentTraceStep;
import com.example.javabigwork.writing.dto.EssayReviewAiPayload;
import com.example.javabigwork.writing.dto.EssayReviewDraft;
import com.example.javabigwork.writing.dto.EssayReviewRequest;
import com.example.javabigwork.writing.dto.SentenceFeedbackView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EssayReviewAiService {
    private final AiSettingsService settingsService;
    private final OpenAiCompatibleClient client;
    private final ObjectMapper objectMapper;

    public EssayReviewAiService(
            AiSettingsService settingsService,
            OpenAiCompatibleClient client,
            ObjectMapper objectMapper
    ) {
        this.settingsService = settingsService;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public Optional<EssayReviewDraft> review(EssayReviewRequest request) {
        AiRuntimeSettings settings = settingsService.resolveCurrent();
        if (!StringUtils.hasText(settings.baseUrl()) || !StringUtils.hasText(settings.chatModel())) {
            return Optional.empty();
        }
        try {
            String raw = client.chat(settings, systemPrompt(), List.of(new OpenAiChatMessage("user", userPrompt(request))));
            EssayReviewAiPayload payload = parse(raw);
            return Optional.of(toDraft(payload, settings.chatModel()));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private String systemPrompt() {
        return """
                You are LinguaMate, an expert English essay teacher.
                Review the student's essay and return only valid JSON.
                Do not use markdown, code fences, or extra commentary.

                Required JSON shape:
                {
                  "totalScore": 0,
                  "grammarScore": 0,
                  "vocabularyScore": 0,
                  "structureScore": 0,
                  "logicScore": 0,
                  "summary": "Chinese summary of the overall writing quality",
                  "polishedText": "revised English essay that preserves the original meaning",
                  "feedback": [
                    {
                      "category": "grammar",
                      "originalSentence": "original sentence or phrase",
                      "suggestion": "Chinese suggestion",
                      "correctedSentence": "corrected English sentence"
                    }
                  ]
                }

                Scoring range is 0 to 100. Use concise Chinese for explanations and natural English for polished text.
                """;
    }

    private String userPrompt(EssayReviewRequest request) {
        String title = StringUtils.hasText(request.title()) ? request.title().trim() : "Untitled Essay";
        String examType = StringUtils.hasText(request.examType()) ? request.examType().trim() : "general";
        String targetLevel = StringUtils.hasText(request.targetLevel()) ? request.targetLevel().trim() : "intermediate";
        return """
                Essay title: %s
                Exam type: %s
                Target level: %s

                Essay:
                %s

                Please score the essay on grammar, vocabulary, structure, and logic.
                Return a polished version and a short, useful feedback list.
                """.formatted(title, examType, targetLevel, request.essayText().trim());
    }

    private EssayReviewAiPayload parse(String raw) throws Exception {
        String json = extractJson(raw);
        EssayReviewAiPayload payload = objectMapper.readValue(json, EssayReviewAiPayload.class);
        validate(payload);
        return payload;
    }

    private String extractJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("模型返回为空");
        }
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("(?s)^```(?:json)?\\s*", "");
            cleaned = cleaned.replaceFirst("(?s)\\s*```$", "");
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1).trim();
        }
        return cleaned;
    }

    private void validate(EssayReviewAiPayload payload) {
        if (payload == null
                || payload.totalScore() == null
                || payload.grammarScore() == null
                || payload.vocabularyScore() == null
                || payload.structureScore() == null
                || payload.logicScore() == null
                || !StringUtils.hasText(payload.summary())
                || !StringUtils.hasText(payload.polishedText())) {
            throw new IllegalArgumentException("模型返回格式不完整");
        }
    }

    private EssayReviewDraft toDraft(EssayReviewAiPayload payload, String modelName) {
        List<SentenceFeedbackView> feedback = payload.feedback() == null ? List.of() : payload.feedback();
        List<AgentTraceStep> trace = List.of(
                new AgentTraceStep("Observe", "读取作文", "当前配置模型：" + modelName),
                new AgentTraceStep("Think", "生成批改建议", "按语法、词汇、结构和逻辑输出结构化结果"),
                new AgentTraceStep("Answer", "返回批改报告", "已生成 AI 批改报告")
        );
        return new EssayReviewDraft(
                clampScore(payload.totalScore()),
                clampScore(payload.grammarScore()),
                clampScore(payload.vocabularyScore()),
                clampScore(payload.structureScore()),
                clampScore(payload.logicScore()),
                payload.summary().trim(),
                payload.polishedText().trim(),
                feedback,
                trace,
                "AI批改",
                modelName
        );
    }

    private double clampScore(Double score) {
        if (score == null || Double.isNaN(score) || Double.isInfinite(score)) {
            return 0;
        }
        return Math.max(0, Math.min(100, score));
    }
}
