package com.example.javabigwork.writing.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.agent.AgentTraceStep;
import com.example.javabigwork.tool.EssayScoringResult;
import com.example.javabigwork.tool.EssayScoringTool;
import com.example.javabigwork.vocabulary.service.ExperienceService;
import com.example.javabigwork.writing.dto.EssayReviewAiPayload;
import com.example.javabigwork.writing.dto.EssayReviewDraft;
import com.example.javabigwork.writing.dto.EssayReviewRequest;
import com.example.javabigwork.writing.dto.EssayReviewResponse;
import com.example.javabigwork.writing.dto.SentenceFeedbackView;
import com.example.javabigwork.writing.entity.EssayFeedback;
import com.example.javabigwork.writing.entity.EssaySubmission;
import com.example.javabigwork.writing.mapper.EssayFeedbackMapper;
import com.example.javabigwork.writing.mapper.EssaySubmissionMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class WritingService {
    private final EssayReviewAiService essayReviewAiService;
    private final EssayScoringTool scoringTool;
    private final EssaySubmissionMapper submissionMapper;
    private final EssayFeedbackMapper feedbackMapper;
    private final ExperienceService experienceService;

    public WritingService(
            EssayReviewAiService essayReviewAiService,
            EssayScoringTool scoringTool,
            EssaySubmissionMapper submissionMapper,
            EssayFeedbackMapper feedbackMapper,
            ExperienceService experienceService
    ) {
        this.essayReviewAiService = essayReviewAiService;
        this.scoringTool = scoringTool;
        this.submissionMapper = submissionMapper;
        this.feedbackMapper = feedbackMapper;
        this.experienceService = experienceService;
    }

    public EssayReviewResponse review(EssayReviewRequest request) {
        EssayReviewDraft draft = essayReviewAiService.review(request).orElseGet(() -> localDraft(request));
        EssaySubmission submission = persistSubmission(request, draft);
        persistFeedback(submission.id, draft.feedback());
        int xp = 20;
        if (draft.totalScore() >= 80) xp += 15;
        experienceService.addXp(xp);
        return toResponse(submission.id, draft);
    }

    public List<EssaySubmission> history() {
        return submissionMapper.selectList(Wrappers.<EssaySubmission>query().orderByDesc("created_at"));
    }

    private EssaySubmission persistSubmission(EssayReviewRequest request, EssayReviewDraft draft) {
        EssaySubmission submission = new EssaySubmission();
        submission.title = request.title() == null || request.title().isBlank() ? "Untitled Essay" : request.title();
        submission.examType = request.examType() == null || request.examType().isBlank() ? "general" : request.examType();
        submission.originalText = request.essayText();
        submission.polishedText = draft.polishedText();
        submission.totalScore = draft.totalScore();
        submission.createdAt = LocalDateTime.now();
        submissionMapper.insert(submission);
        return submission;
    }

    private void persistFeedback(Long essayId, List<SentenceFeedbackView> feedbackList) {
        if (feedbackList == null) {
            return;
        }
        feedbackList.forEach(item -> {
            EssayFeedback feedback = new EssayFeedback();
            feedback.essayId = essayId;
            feedback.category = item.category();
            feedback.originalSentence = item.originalSentence();
            feedback.suggestion = item.suggestion();
            feedback.correctedSentence = item.correctedSentence();
            feedbackMapper.insert(feedback);
        });
    }

    private EssayReviewDraft localDraft(EssayReviewRequest request) {
        EssayScoringResult scoring = scoringTool.score(request);
        List<AgentTraceStep> trace = List.of(
                new AgentTraceStep("Observe", "读取作文", "作文类型：" + safeValue(request.examType(), "general")),
                new AgentTraceStep("Think", "使用本地评分器", "在线模型未启用或调用失败，自动切换到本地批改"),
                new AgentTraceStep("Answer", "返回批改报告", "总分：" + Math.round(scoring.totalScore()))
        );
        return new EssayReviewDraft(
                scoring.totalScore(),
                scoring.grammarScore(),
                scoring.vocabularyScore(),
                scoring.structureScore(),
                scoring.logicScore(),
                scoring.summary(),
                scoring.polishedText(),
                scoring.feedback(),
                trace,
                "本地兜底",
                "local-rules"
        );
    }

    private EssayReviewResponse toResponse(Long essayId, EssayReviewDraft draft) {
        return new EssayReviewResponse(
                essayId,
                draft.totalScore(),
                draft.grammarScore(),
                draft.vocabularyScore(),
                draft.structureScore(),
                draft.logicScore(),
                draft.summary(),
                draft.polishedText(),
                draft.feedback(),
                draft.trace(),
                draft.reviewSource(),
                draft.modelName()
        );
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
