package com.example.javabigwork.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.javabigwork.agent.dto.ChatAction;
import com.example.javabigwork.agent.dto.LearningAdvisorView;
import com.example.javabigwork.agent.dto.LearningCard;
import com.example.javabigwork.agent.dto.LearningCardItem;
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

    public LearningAdvisorService(
            UserContext userContext,
            ConversationMapper conversationMapper,
            MessageMapper messageMapper,
            WordReviewLogMapper wordReviewLogMapper,
            UserWordProgressMapper userWordProgressMapper,
            WordMapper wordMapper,
            EssaySubmissionMapper essaySubmissionMapper,
            ReadingRecordMapper readingRecordMapper) {
        this.userContext = userContext;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.wordReviewLogMapper = wordReviewLogMapper;
        this.userWordProgressMapper = userWordProgressMapper;
        this.wordMapper = wordMapper;
        this.essaySubmissionMapper = essaySubmissionMapper;
        this.readingRecordMapper = readingRecordMapper;
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
        addAction(actions, items.get(2), "复习错词", "/review", Map.of("mode", "today"));
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
                .map(progress -> wordItem(words.get(progress.wordId), progress, group))
                .sorted(Comparator.comparing(LearningCardItem::label, String.CASE_INSENSITIVE_ORDER))
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
        ChatAction action = new ChatAction(
                "open-" + group,
                mastered ? "查看已掌握" : "开始复习",
                "review",
                "/review",
                Map.of("mode", mastered ? "mastered" : "today"));
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
        long userMessages = messageMapper.selectCount(new QueryWrapper<Message>()
                .in("conversation_id", conversationIds)
                .eq("role", "user")
                .ge("created_at", start)
                .lt("created_at", end));
        long assistantMessages = messageMapper.selectCount(new QueryWrapper<Message>()
                .in("conversation_id", conversationIds)
                .eq("role", "assistant")
                .ge("created_at", start)
                .lt("created_at", end));
        return userMessages > 0 && assistantMessages > 0;
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
