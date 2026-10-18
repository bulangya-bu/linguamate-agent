package com.example.javabigwork.writing.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.agent.AgentTraceStep;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.tool.EssayScoringResult;
import com.example.javabigwork.tool.EssayScoringTool;
import com.example.javabigwork.vocabulary.service.ExperienceService;
import com.example.javabigwork.writing.dto.EssayReviewDraft;
import com.example.javabigwork.writing.dto.EssayReviewRequest;
import com.example.javabigwork.writing.dto.EssayReviewResponse;
import com.example.javabigwork.writing.dto.SentenceFeedbackView;
import com.example.javabigwork.writing.dto.RevisionComparison;
import com.example.javabigwork.writing.entity.EssayFeedback;
import com.example.javabigwork.writing.entity.EssaySubmission;
import com.example.javabigwork.writing.mapper.EssayFeedbackMapper;
import com.example.javabigwork.writing.mapper.EssaySubmissionMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
public class WritingService {
    private final EssayReviewAiService essayReviewAiService;
    private final EssayScoringTool scoringTool;
    private final EssaySubmissionMapper submissionMapper;
    private final EssayFeedbackMapper feedbackMapper;
    private final ExperienceService experienceService;
    private final UserContext userContext;

    public WritingService(
            EssayReviewAiService essayReviewAiService,
            EssayScoringTool scoringTool,
            EssaySubmissionMapper submissionMapper,
            EssayFeedbackMapper feedbackMapper,
            ExperienceService experienceService,
            UserContext userContext
    ) {
        this.essayReviewAiService = essayReviewAiService;
        this.scoringTool = scoringTool;
        this.submissionMapper = submissionMapper;
        this.feedbackMapper = feedbackMapper;
        this.experienceService = experienceService;
        this.userContext = userContext;
    }

    @Transactional
    public EssayReviewResponse review(EssayReviewRequest request) {
        EssaySubmission previous = null;
        if (request.revisionOfId() != null) {
            previous = submissionMapper.selectOne(Wrappers.<EssaySubmission>query()
                    .eq("id", request.revisionOfId()).eq("user_id", userContext.userId()));
            if (previous == null) throw new IllegalArgumentException("未找到可改写的作文");
            if (previous.originalText.trim().equals(request.essayText().trim())) {
                throw new IllegalArgumentException("请先修改作文，再提交复评");
            }
        }
        EssayReviewDraft draft = essayReviewAiService.review(request).orElseGet(() -> localDraft(request));
        EssaySubmission submission = persistSubmission(request, draft);
        persistFeedback(submission.id, draft.feedback());
        int xp = 20;
        if (draft.totalScore() >= 80) xp += 15;
        experienceService.addXp(xp);
        RevisionComparison comparison = null;
        if (previous != null) {
            boolean comparable = Objects.equals(previous.reviewSource, draft.reviewSource())
                    && Objects.equals(previous.modelName, draft.modelName())
                    && Objects.equals(previous.examType, submission.examType)
                    && Objects.equals(previous.targetLevel, submission.targetLevel);
            comparison = new RevisionComparison(previous.id, previous.totalScore, draft.totalScore(), comparable,
                    comparable ? ("本地兜底".equals(draft.reviewSource()) ? "本地规则评分，仅供练习参考" : "同一模型与评分设置，分数仅供参考")
                            : "模型、来源或评分设置发生变化，仅并列展示");
        }
        return toResponse(submission.id, draft, comparison);
    }

    public List<EssaySubmission> history() {
        return submissionMapper.selectList(Wrappers.<EssaySubmission>query()
                .eq("user_id", userContext.userId())
                .orderByDesc("created_at"));
    }

    private EssaySubmission persistSubmission(EssayReviewRequest request, EssayReviewDraft draft) {
        EssaySubmission submission = new EssaySubmission();
        submission.userId = userContext.userId();
        submission.title = request.title() == null || request.title().isBlank() ? "Untitled Essay" : request.title();
        submission.examType = request.examType() == null || request.examType().isBlank() ? "general" : request.examType();
        submission.originalText = request.essayText();
        submission.polishedText = draft.polishedText();
        submission.totalScore = draft.totalScore();
        submission.revisionOfId = request.revisionOfId();
        submission.reviewSource = draft.reviewSource();
        submission.modelName = draft.modelName();
        submission.targetLevel = safeValue(request.targetLevel(), "B1");
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

    private EssayReviewResponse toResponse(Long essayId, EssayReviewDraft draft, RevisionComparison comparison) {
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
                draft.modelName(),
                comparison
        );
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
