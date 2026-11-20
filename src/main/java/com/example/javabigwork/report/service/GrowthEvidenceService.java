package com.example.javabigwork.report.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.agent.dto.CoachFeedback;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.chat.entity.Conversation;
import com.example.javabigwork.chat.entity.Message;
import com.example.javabigwork.chat.mapper.ConversationMapper;
import com.example.javabigwork.chat.mapper.MessageMapper;
import com.example.javabigwork.mission.service.MissionService;
import com.example.javabigwork.news.entity.NewsArticle;
import com.example.javabigwork.news.entity.ReadingRecord;
import com.example.javabigwork.news.mapper.NewsArticleMapper;
import com.example.javabigwork.news.mapper.ReadingRecordMapper;
import com.example.javabigwork.report.dto.GrowthEvidenceView;
import com.example.javabigwork.report.dto.GrowthEvidenceView.Attempt;
import com.example.javabigwork.report.dto.GrowthEvidenceView.ExpressionEvidence;
import com.example.javabigwork.report.dto.GrowthEvidenceView.ReadingEvidence;
import com.example.javabigwork.report.dto.GrowthEvidenceView.WordEvidence;
import com.example.javabigwork.report.dto.GrowthEvidenceView.WritingEvidence;
import com.example.javabigwork.vocabulary.entity.WordReviewLog;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import com.example.javabigwork.vocabulary.mapper.WordReviewLogMapper;
import com.example.javabigwork.writing.entity.EssaySubmission;
import com.example.javabigwork.writing.mapper.EssaySubmissionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GrowthEvidenceService {
    private final UserContext userContext;
    private final WordReviewLogMapper logs;
    private final WordMapper words;
    private final EssaySubmissionMapper essays;
    private final MissionService missions;
    private final boolean missionsEnabled;
    private final MessageMapper messages;
    private final ConversationMapper conversations;
    private final ReadingRecordMapper readingRecords;
    private final NewsArticleMapper newsArticles;
    private final ObjectMapper json;

    public GrowthEvidenceService(UserContext userContext, WordReviewLogMapper logs, WordMapper words,
            EssaySubmissionMapper essays, MissionService missions, MessageMapper messages,
            ConversationMapper conversations, ReadingRecordMapper readingRecords, NewsArticleMapper newsArticles,
            ObjectMapper json, @Value("${linguamate.missions.enabled:false}") boolean missionsEnabled) {
        this.userContext = userContext;
        this.logs = logs;
        this.words = words;
        this.essays = essays;
        this.missions = missions;
        this.messages = messages;
        this.conversations = conversations;
        this.readingRecords = readingRecords;
        this.newsArticles = newsArticles;
        this.json = json;
        this.missionsEnabled = missionsEnabled;
    }

    public GrowthEvidenceView evidence() {
        Long userId = userContext.userId();
        var writing = essays.selectList(Wrappers.<EssaySubmission>query().eq("user_id", userId)
                .orderByDesc("created_at", "id").last("LIMIT 8"))
                .stream().map(item -> writing(item, userId)).toList();
        return new GrowthEvidenceView(wordEvidence(userId), writing,
                missionsEnabled ? missions.history().stream().limit(8).toList() : List.of(),
                expressions(userId), readings(userId));
    }

    private List<WordEvidence> wordEvidence(Long userId) {
        var recent = logs.selectList(Wrappers.<WordReviewLog>query().eq("user_id", userId)
                .in("review_type", "QUIZ", "STRICT_REVIEW").orderByDesc("created_at", "id").last("LIMIT 400"));
        Map<Long, List<WordReviewLog>> groups = new LinkedHashMap<>();
        for (var log : recent) {
            groups.computeIfAbsent(log.wordId, key -> new ArrayList<>()).add(log);
        }
        List<WordEvidence> result = new ArrayList<>();
        for (var entry : groups.entrySet()) {
            var word = words.selectById(entry.getKey());
            if (word == null) continue;
            var attempts = entry.getValue().stream().limit(8)
                    .sorted(Comparator.comparing((WordReviewLog item) -> item.createdAt,
                            Comparator.nullsFirst(Comparator.naturalOrder())).thenComparing(item -> item.id))
                    .map(item -> new Attempt(item.id, item.answer, Boolean.TRUE.equals(item.correct),
                            item.reviewType, item.createdAt, item.spellingAnswer, item.meaningAnswer,
                            item.spellingCorrect, item.meaningCorrect))
                    .toList();
            result.add(new WordEvidence(word.id, word.word, word.meaningCn, attempts));
            if (result.size() == 16) break;
        }
        return result;
    }

    private List<ExpressionEvidence> expressions(Long userId) {
        var owned = conversations.selectList(Wrappers.<Conversation>query().eq("user_id", userId)
                .orderByDesc("updated_at", "id").last("LIMIT 30"));
        if (owned.isEmpty()) return List.of();
        var threads = owned.stream().collect(Collectors.toMap(conversation -> conversation.id, conversation -> conversation));
        var rows = messages.selectList(Wrappers.<Message>query().in("conversation_id", threads.keySet())
                .orderByDesc("created_at", "id").last("LIMIT 120"));
        List<ExpressionEvidence> result = new ArrayList<>();
        for (var row : rows) {
            if (!"user".equals(row.role)) continue;
            String mode = messageMode(row.metadata);
            if ("assistant".equals(mode)) continue;
            var next = rows.stream().filter(message -> message.conversationId.equals(row.conversationId)
                            && message.id > row.id)
                    .min(Comparator.comparing(message -> message.id)).orElse(null);
            if (next != null && !"assistant".equals(next.role)) next = null;
            var thread = threads.get(row.conversationId);
            String title = thread.title == null ? "学习对话" : thread.title;
            result.add(new ExpressionEvidence(row.id, row.conversationId, title, row.content,
                    next == null ? null : next.content, next == null ? null : coachFeedback(next.metadata),
                    row.createdAt, mode, thread.scene));
            if (result.size() == 12) break;
        }
        return result;
    }

    private String messageMode(String metadata) {
        if (metadata == null || metadata.isBlank()) return null;
        try {
            return json.readTree(metadata).path("mode").asText(null);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private CoachFeedback coachFeedback(String metadata) {
        if (metadata == null || metadata.isBlank()) return null;
        try {
            var node = json.readTree(metadata).path("coaching");
            return node.isObject() ? json.treeToValue(node, CoachFeedback.class) : null;
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private List<ReadingEvidence> readings(Long userId) {
        var recent = readingRecords.selectList(Wrappers.<ReadingRecord>query().eq("user_id", userId)
                .apply(ReadingRecord.HAS_ACTIVITY).isNotNull("article_id")
                .orderByDesc("updated_at", "id").last("LIMIT 18"));
        if (recent.isEmpty()) return List.of();

        Map<Long, NewsArticle> articles = newsArticles.selectList(Wrappers.<NewsArticle>query()
                        .select("id", "title", "summary", "source_name", "difficulty")
                        .in("id", recent.stream().map(record -> record.articleId).toList()))
                .stream().collect(Collectors.toMap(article -> article.id, article -> article));
        List<ReadingEvidence> result = new ArrayList<>();
        for (var record : recent) {
            NewsArticle article = articles.get(record.articleId);
            if (article == null) continue;
            result.add(new ReadingEvidence(article.id, article.title, article.summary, article.sourceName,
                    article.difficulty, Objects.requireNonNullElse(record.readProgress, 0),
                    Objects.requireNonNullElse(record.lookedUpWords, 0), Boolean.TRUE.equals(record.finished),
                    record.finishedAt, record.updatedAt));
        }
        return result;
    }

    private WritingEvidence writing(EssaySubmission current, Long userId) {
        var previous = current.revisionOfId == null ? null : essays.selectOne(Wrappers.<EssaySubmission>query()
                .eq("id", current.revisionOfId).eq("user_id", userId));
        boolean comparable = previous != null && previous.totalScore != null && current.totalScore != null
                && current.reviewSource != null && current.modelName != null
                && Objects.equals(previous.reviewSource, current.reviewSource)
                && Objects.equals(previous.modelName, current.modelName)
                && Objects.equals(previous.examType, current.examType)
                && Objects.equals(previous.targetLevel, current.targetLevel);
        return new WritingEvidence(current.id, current.title, previous == null ? null : previous.id,
                previous == null ? "" : previous.originalText, current.originalText, current.polishedText,
                previous == null ? null : previous.totalScore, current.totalScore, comparable, current.reviewSource,
                current.modelName, current.createdAt);
    }
}
