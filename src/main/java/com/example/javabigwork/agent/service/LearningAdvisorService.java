package com.example.javabigwork.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.javabigwork.agent.dto.ChatAction;
import com.example.javabigwork.agent.dto.LearningAdvisorView;
import com.example.javabigwork.agent.dto.LearningCard;
import com.example.javabigwork.agent.dto.LearningCardItem;
import com.example.javabigwork.agent.dto.LearningAssistantSnapshot;
import com.example.javabigwork.agent.dto.LearningAssistantSnapshot.WordEvidence;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.chat.entity.Conversation;
import com.example.javabigwork.chat.entity.Message;
import com.example.javabigwork.chat.mapper.ConversationMapper;
import com.example.javabigwork.chat.mapper.MessageMapper;
import com.example.javabigwork.news.entity.ReadingRecord;
import com.example.javabigwork.news.mapper.ReadingRecordMapper;
import com.example.javabigwork.vocabulary.entity.UserWordProgress;
import com.example.javabigwork.vocabulary.entity.Word;
import com.example.javabigwork.vocabulary.entity.WordReviewLog;
import com.example.javabigwork.vocabulary.mapper.UserWordProgressMapper;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import com.example.javabigwork.vocabulary.mapper.WordReviewLogMapper;
import com.example.javabigwork.writing.entity.EssaySubmission;
import com.example.javabigwork.writing.mapper.EssaySubmissionMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LearningAdvisorService {

    private final UserContext userContext;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final WordReviewLogMapper wordReviewLogMapper;
    private final UserWordProgressMapper userWordProgressMapper;
    private final WordMapper wordMapper;
    private final EssaySubmissionMapper essaySubmissionMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final ObjectMapper json;

    public LearningAdvisorService(
            UserContext userContext,
            ConversationMapper conversationMapper,
            MessageMapper messageMapper,
            WordReviewLogMapper wordReviewLogMapper,
            UserWordProgressMapper userWordProgressMapper,
            WordMapper wordMapper,
            EssaySubmissionMapper essaySubmissionMapper,
            ReadingRecordMapper readingRecordMapper,
            ObjectMapper json) {
        this.userContext = userContext;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.wordReviewLogMapper = wordReviewLogMapper;
        this.userWordProgressMapper = userWordProgressMapper;
        this.wordMapper = wordMapper;
        this.essaySubmissionMapper = essaySubmissionMapper;
        this.readingRecordMapper = readingRecordMapper;
        this.json = json;
    }

    public LearningAssistantSnapshot assistantSnapshot() {
        LocalDateTime now = LocalDateTime.now();
        List<UserWordProgress> progresses = userWordProgressMapper.selectList(
                new QueryWrapper<UserWordProgress>().eq("user_id", userContext.userId()));
        List<UserWordProgress> priorities = progresses.stream()
                .filter(progress -> unfamiliar(progress) || due(progress, now))
                .sorted(priorityOrder(now))
                .limit(12).toList();
        Map<Long, Word> words = new HashMap<>();
        if (!priorities.isEmpty()) {
            wordMapper.selectBatchIds(priorities.stream().map(progress -> progress.wordId).toList())
                    .forEach(word -> words.put(word.id, word));
        }
        List<WordEvidence> evidence = priorities.stream().filter(progress -> words.containsKey(progress.wordId))
                .map(progress -> new WordEvidence(words.get(progress.wordId).word,
                        words.get(progress.wordId).meaningCn, value(progress.familiarity), value(progress.wrongCount),
                        value(progress.reviewCount), due(progress, now), progress.lastReviewAt, progress.nextReviewAt))
                .toList();
        long unfamiliarCount = progresses.stream().filter(this::unfamiliar).count();
        return new LearningAssistantSnapshot(now.toLocalDate().toString(), dailyRoute(false).learningCard(),
                progresses.size(), unfamiliarCount, progresses.size() - unfamiliarCount,
                progresses.stream().filter(progress -> value(progress.wrongCount) > 0).count(),
                progresses.stream().filter(progress -> due(progress, now)).count(), evidence);
    }

    private boolean unfamiliar(UserWordProgress progress) {
        return value(progress.familiarity) < 3 || value(progress.wrongCount) > 0;
    }

    private boolean due(UserWordProgress progress, LocalDateTime now) {
        return progress.nextReviewAt != null && !progress.nextReviewAt.isAfter(now);
    }

    private Comparator<UserWordProgress> priorityOrder(LocalDateTime now) {
        return Comparator.<UserWordProgress>comparingInt(progress -> value(progress.wrongCount)).reversed()
                .thenComparing(progress -> !due(progress, now))
                .thenComparingInt(progress -> value(progress.familiarity))
                .thenComparing(progress -> progress.lastReviewAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(progress -> progress.wordId);
    }

    public LearningAdvisorView dailyRoute(boolean includeCurrentExchange) {
        Long userId = userContext.userId();
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        boolean chatComplete = includeCurrentExchange || hasCompletedConversation(userId, start, end);
        long quizCount = wordReviewLogMapper.selectCount(new QueryWrapper<WordReviewLog>()
                .eq("user_id", userId)
                .eq("review_type", "QUIZ")
                .ge("created_at", start)
                .lt("created_at", end));
        long reviewCount = userWordProgressMapper.selectCount(new QueryWrapper<UserWordProgress>()
                .eq("user_id", userId)
                .gt("wrong_count", 0));
        long essayCount = essaySubmissionMapper.selectCount(new QueryWrapper<EssaySubmission>()
                .eq("user_id", userId)
                .ge("created_at", start)
                .lt("created_at", end));
        long readingCount = readingRecordMapper.selectCount(new QueryWrapper<ReadingRecord>()
                .eq("user_id", userId)
                .eq("finished", true)
                .ge("updated_at", start)
                .lt("updated_at", end));

        List<LearningCardItem> items = List.of(
                item("chat", "对话练习", chatComplete ? "今天已完成 1 次对话" : "完成 1 次对话练习", chatComplete),
                item("vocabulary", "单词学习", quizCount + "/6 次测验", quizCount >= 6),
                item("review", "错词复习", reviewCount == 0 ? "今日错词已清空" : "还有 " + reviewCount + " 个错词", reviewCount == 0),
                item("writing", "写作练习", essayCount + "/1 篇作文", essayCount >= 1),
                item("news", "新闻阅读", readingCount + "/1 篇阅读", readingCount >= 1));

        List<ChatAction> actions = new ArrayList<>();
        addAction(actions, items.get(0), "开始对话", "/chat", Map.of());
        addAction(actions, items.get(1), "学习单词", "/vocabulary", Map.of());
        addAction(actions, items.get(2), "复习错词", "/review", Map.of("mode", "mistakes"));
        addAction(actions, items.get(3), "开始写作", "/writing", Map.of());
        addAction(actions, items.get(4), "阅读新闻", "/news", Map.of());

        long completed = items.stream().filter(item -> "complete".equals(item.status())).count();
        String summary = completed == items.size()
                ? "今天的学习路径已经全部完成。"
                : "今天已完成 " + completed + "/" + items.size() + " 项，建议继续完成下面的任务。";
        return new LearningAdvisorView(
                summary,
                new LearningCard("daily-route", "今日学习路径", summary, "daily", items, 0,
                        items.size(), items.size(), 1),
                List.copyOf(actions));
    }

    public LearningAdvisorView vocabularySummary(String group, int requestedPage, int requestedSize) {
        if (!"mastered".equals(group) && !"unfamiliar".equals(group)) {
            throw new IllegalArgumentException("词库分组仅支持 mastered 或 unfamiliar");
        }
        if (requestedPage < 0) {
            throw new IllegalArgumentException("页码不能小于 0");
        }
        int size = Math.max(1, Math.min(requestedSize, 20));
        List<UserWordProgress> progresses = userWordProgressMapper.selectList(
                new QueryWrapper<UserWordProgress>().eq("user_id", userContext.userId()));
        List<UserWordProgress> selected = progresses.stream()
                .filter(progress -> "mastered".equals(group)
                        ? value(progress.familiarity) >= 3 && value(progress.wrongCount) == 0
                        : value(progress.familiarity) < 3 || value(progress.wrongCount) > 0)
                .toList();

        Map<Long, Word> words = new HashMap<>();
        if (!selected.isEmpty()) {
            wordMapper.selectBatchIds(selected.stream().map(progress -> progress.wordId).toList())
                    .forEach(word -> words.put(word.id, word));
        }
        List<LearningCardItem> allItems = selected.stream()
                .filter(progress -> words.containsKey(progress.wordId))
                .sorted("mastered".equals(group)
                        ? Comparator.comparing(progress -> words.get(progress.wordId).word, String.CASE_INSENSITIVE_ORDER)
                        : priorityOrder(LocalDateTime.now()))
                .map(progress -> wordItem(words.get(progress.wordId), progress, group))
                .toList();

        int totalPages = allItems.isEmpty() ? 0 : (allItems.size() + size - 1) / size;
        int page = totalPages == 0 ? 0 : Math.min(requestedPage, totalPages - 1);
        int from = Math.min(page * size, allItems.size());
        int to = Math.min(from + size, allItems.size());
        List<LearningCardItem> items = allItems.subList(from, to);
        boolean mastered = "mastered".equals(group);
        String title = mastered ? "已掌握单词" : "不熟悉单词";
        String summary = mastered
                ? "词库中有 " + allItems.size() + " 个已掌握单词。"
                : "词库中有 " + allItems.size() + " 个需要继续巩固的单词。";
        boolean hasMistakes = selected.stream().anyMatch(progress -> value(progress.wrongCount) > 0);
        boolean hasDue = selected.stream().anyMatch(progress -> due(progress, LocalDateTime.now()));
        String route = mastered ? "/wordbank" : hasMistakes || hasDue ? "/review" : "/vocabulary";
        ChatAction action = new ChatAction(
                "open-" + group,
                mastered ? "查看已掌握" : hasMistakes ? "复习错词" : hasDue ? "到期复习" : "继续练词",
                route.substring(1),
                route,
                mastered ? Map.of("scope", "mastered") : hasMistakes || hasDue
                        ? Map.of("mode", hasMistakes ? "mistakes" : "today") : Map.of());
        return new LearningAdvisorView(
                summary,
                new LearningCard("vocabulary-summary", title, summary, group, List.copyOf(items), page,
                        size, allItems.size(), totalPages),
                List.of(action));
    }

    private boolean hasCompletedConversation(Long userId, LocalDateTime start, LocalDateTime end) {
        List<Long> conversationIds = conversationMapper.selectList(new QueryWrapper<Conversation>()
                        .eq("user_id", userId))
                .stream().map(conversation -> conversation.id).toList();
        if (conversationIds.isEmpty()) {
            return false;
        }
        List<Message> messages = messageMapper.selectList(new QueryWrapper<Message>()
                .in("conversation_id", conversationIds)
                .ge("created_at", start)
                .lt("created_at", end));
        return messages.stream().filter(message -> "user".equals(message.role) && practiceMessage(message))
                .anyMatch(user -> messages.stream().anyMatch(answer -> user.conversationId.equals(answer.conversationId)
                        && "assistant".equals(answer.role) && practiceMessage(answer)));
    }

    private boolean practiceMessage(Message message) {
        // Pre-mode legacy conversations had no metadata and remain valid learning evidence.
        if (message.metadata == null || message.metadata.isBlank()) return true;
        try {
            var metadata = json.readTree(message.metadata);
            if ("user".equals(message.role)) return "practice".equals(metadata.path("mode").asText());
            return metadata.hasNonNull("coaching");
        } catch (Exception ignored) {
            return false;
        }
    }

    private LearningCardItem item(String key, String label, String detail, boolean complete) {
        return new LearningCardItem(key, label, detail, complete ? "complete" : "incomplete");
    }

    private LearningCardItem wordItem(Word word, UserWordProgress progress, String group) {
        String meaning = word.meaningCn == null ? "暂无释义" : word.meaningCn;
        String detail = meaning + " · 熟悉度 " + value(progress.familiarity)
                + (value(progress.wrongCount) > 0 ? " · 错误 " + value(progress.wrongCount) + " 次" : "");
        return new LearningCardItem(String.valueOf(word.id), word.word, detail, group);
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private void addAction(
            List<ChatAction> actions,
            LearningCardItem item,
            String label,
            String route,
            Map<String, String> query) {
        if (!"complete".equals(item.status())) {
            actions.add(new ChatAction(item.key(), label, item.key(), route, query));
        }
    }
}
