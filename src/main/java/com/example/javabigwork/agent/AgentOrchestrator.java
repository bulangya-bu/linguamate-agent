package com.example.javabigwork.agent;

import com.example.javabigwork.ai.service.WebAnswerService;
import com.example.javabigwork.agent.dto.ChatAction;
import com.example.javabigwork.agent.dto.LearningAdvisorView;
import com.example.javabigwork.agent.dto.LearningCard;
import com.example.javabigwork.agent.dto.LearningAssistantSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.javabigwork.agent.service.LearningAdvisorService;
import com.example.javabigwork.chat.entity.Message;
import com.example.javabigwork.config.RagConfig.RagRuntimeOptions;
import com.example.javabigwork.knowledge.dto.KnowledgeSearchResult;
import com.example.javabigwork.knowledge.service.RetrievalService;
import com.example.javabigwork.tool.DictionaryTool;
import com.example.javabigwork.tool.NewsRecommendTool;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgentOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);
    private final AgentTraceRecorder traceRecorder;
    private final AgentMemoryService memoryService;
    private final DictionaryTool dictionaryTool;
    private final NewsRecommendTool newsRecommendTool;
    private final WebAnswerService webAnswerService;
    private final RetrievalService retrievalService;
    private final RagRuntimeOptions ragOptions;
    private final LearningAdvisorService learningAdvisorService;
    private final ObjectMapper json;
    private static final List<Pattern> WORD_QUERIES = List.of(
            Pattern.compile("^(?:请|帮我|请帮我)?\\s*(?:查一下|查查|查询|查|解释一下|解释|单词|word|define)\\s*[：:\\\"“']?([a-z]+(?:['-][a-z]+)*)[\\\"”']?\\s*(?:这个词|的意思|的释义|是什么意思)?[。？?！!\\s]*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^what does\\s+[\\\"']?([a-z]+(?:['-][a-z]+)*)[\\\"']?\\s+mean[?!.\\s]*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^[\\\"“']?([a-z]+(?:['-][a-z]+)*)[\\\"”']?\\s*(?:这个词)?(?:是什么意思|怎么读|的释义|的意思)[。？?！!\\s]*$", Pattern.CASE_INSENSITIVE));

    public AgentOrchestrator(
            AgentTraceRecorder traceRecorder,
            AgentMemoryService memoryService,
            DictionaryTool dictionaryTool,
            NewsRecommendTool newsRecommendTool,
            WebAnswerService webAnswerService,
            RetrievalService retrievalService,
            RagRuntimeOptions ragOptions,
            LearningAdvisorService learningAdvisorService,
            ObjectMapper json
    ) {
        this.traceRecorder = traceRecorder;
        this.memoryService = memoryService;
        this.dictionaryTool = dictionaryTool;
        this.newsRecommendTool = newsRecommendTool;
        this.webAnswerService = webAnswerService;
        this.retrievalService = retrievalService;
        this.ragOptions = ragOptions;
        this.learningAdvisorService = learningAdvisorService;
        this.json = json;
    }

    public AgentResult handle(String scene, String message, List<Message> history) {
        log.info("Agent handle: scene={}, intent decision start", scene);
        List<AgentTraceStep> trace = traceRecorder.start(message);
        String normalized = message.toLowerCase(Locale.ROOT);
        Intent intent = decideIntent(normalized);
        log.info("Agent intent: scene={}, intent={}, message={}", scene, intent, message);
        traceRecorder.add(trace, "Think", "判断任务类型", intent.label);

        List<KnowledgeSearchResult> snippets = retrieveKnowledge(intent, message, normalized, trace);

        String fallbackReply;
        LearningCard learningCard = null;
        List<ChatAction> actions = List.of();
        Map<String, Object> evidence = new LinkedHashMap<>();
        LearningAssistantSnapshot snapshot = learningSnapshot(trace);
        if (snapshot != null) evidence.put("learningRecords", snapshot);
        if (intent == Intent.DAILY_ROUTE || intent == Intent.VOCAB_UNFAMILIAR || intent == Intent.VOCAB_MASTERED) {
            LearningAdvisorView view = advisorView(intent, trace);
            fallbackReply = view.reply();
            if (intent == Intent.VOCAB_UNFAMILIAR && snapshot != null) fallbackReply += prioritySuggestion(snapshot);
            learningCard = view.learningCard();
            actions = view.actions();
            evidence.put("selectedLearningCard", view);
        } else if (intent == Intent.WORD) {
            String word = lookupWord(normalized);
            log.info("Agent tool: DictionaryTool.lookup word={}", word);
            var result = dictionaryTool.lookup(word);
            traceRecorder.add(trace, "Act", "调用 DictionaryTool", "查询单词：" + result.word());
            evidence.put("dictionary", result);
            fallbackReply = "单词 " + result.word() + " 的常用释义是：" + result.meaningCn()
                    + "。音标：" + result.phoneticUs() + "。例句：" + result.exampleEn();
        } else if (intent == Intent.NEWS) {
            log.info("Agent tool: NewsRecommendTool.recommend");
            var articles = newsRecommendTool.recommend("easy", "", 2);
            traceRecorder.add(trace, "Act", "调用 NewsRecommendTool", "推荐 easy 难度英语新闻");
            evidence.put("recommendedArticles", articles.stream().map(article -> Map.of(
                    "title", article.title, "difficulty", article.difficulty, "id", article.id)).toList());
            actions = List.of(new ChatAction("read", "前往阅读", "news", "/news", Map.of()));
            fallbackReply = articles.isEmpty()
                    ? "我会为你推荐 easy 难度的英语新闻。当前新闻库还没有数据，可以先导入新闻材料。"
                    : "推荐你先读：" + articles.get(0).title + "。阅读时把鼠标放在单词上，就能查看中文和音标。";
        } else if (intent == Intent.ESSAY) {
            log.info("Agent tool: redirect to essay page");
            traceRecorder.add(trace, "Act", "准备作文批改", "在 Writing 页面提交完整作文后，系统会返回评分、反馈和润色版本。");
            actions = List.of(new ChatAction("write", "打开写作工坊", "writing", "/writing", Map.of()));
            fallbackReply = "把作文发到写作工坊，可以按语法、词汇、结构和逻辑四个维度批改，并获得润色版本。"
                    + localKnowledgeHint(snippets);
        } else {
            fallbackReply = "暂时无法针对这条问题生成回答。请稍后重试；学习记录和已有对话仍会保留。"
                    + localKnowledgeHint(snippets);
        }

        // Tools supply evidence and navigation, never replace the learner's request or conversation.
        String localReply = fallbackReply;
        String reply = webAnswerService.answer(scene, message, history, snippets, evidenceJson(evidence))
                .map(answer -> {
                    traceRecorder.add(trace, "Act", "调用在线模型", "结合当前问题、最近对话和当前用户的学习记录生成回答");
                    return answer;
                })
                .orElseGet(() -> {
                    traceRecorder.add(trace, "Act", "使用本地兜底", "本轮未获得模型回复，仅展示本地工具和真实记录");
                    return "本轮模型暂不可用。\n" + localReply;
                });

        traceRecorder.add(trace, "Answer", "生成回复", reply);
        log.debug("Agent result: replyLen={}, traceSteps={}", reply.length(), trace.size());
        return new AgentResult(reply, trace, memoryService.defaultSuggestions(), learningCard, actions);
    }

    private LearningAssistantSnapshot learningSnapshot(List<AgentTraceStep> trace) {
        try {
            LearningAssistantSnapshot snapshot = learningAdvisorService.assistantSnapshot();
            traceRecorder.add(trace, "Act", "读取学习档案", "读取今日进度及最多 12 个优先巩固词；保留错误、到期与掌握状态的区别");
            return snapshot;
        } catch (RuntimeException exception) {
            log.warn("Learning snapshot unavailable: {}", exception.getClass().getSimpleName());
            traceRecorder.add(trace, "Act", "读取学习档案", "当前记录读取失败，模型将明确说明缺少依据");
            return null;
        }
    }

    private String evidenceJson(Map<String, Object> evidence) {
        try {
            return json.writeValueAsString(evidence);
        } catch (Exception exception) {
            log.warn("Cannot serialize learning evidence: {}", exception.getClass().getSimpleName());
            return "{}";
        }
    }

    private String prioritySuggestion(LearningAssistantSnapshot snapshot) {
        if (snapshot.priorityWords().isEmpty()) return " 目前没有可用于推荐具体薄弱词的记录，可以先完成一轮单词练习。";
        return "\n优先巩固：" + String.join("；", snapshot.priorityWords().stream().limit(3)
                .map(word -> word.word() + "（错误 " + word.wrongCount() + " 次，熟悉度 " + word.familiarity()
                        + (word.due() ? "，已到复习时间" : "") + "）").toList())
                + "。先回忆释义，再各造一句话。";
    }

    private LearningAdvisorView advisorView(Intent intent, List<AgentTraceStep> trace) {
        try {
            LearningAdvisorView view = switch (intent) {
                case DAILY_ROUTE -> learningAdvisorService.dailyRoute(false);
                case VOCAB_UNFAMILIAR -> learningAdvisorService.vocabularySummary("unfamiliar", 0, 10);
                case VOCAB_MASTERED -> learningAdvisorService.vocabularySummary("mastered", 0, 10);
                default -> throw new IllegalStateException("Unsupported advisor intent: " + intent);
            };
            traceRecorder.add(trace, "Act", "读取学习档案", "根据当前用户的真实学习记录生成结构化建议");
            return view;
        } catch (RuntimeException exception) {
            log.warn("Learning advisor failed: intent={}, error={}", intent, exception.getMessage());
            traceRecorder.add(trace, "Act", "读取学习档案", "学习记录暂时不可用，返回本地说明");
            return new LearningAdvisorView("暂时无法读取你的学习记录，请稍后再试。", null, List.of());
        }
    }

    private List<KnowledgeSearchResult> retrieveKnowledge(
            Intent intent,
            String message,
            String normalized,
            List<AgentTraceStep> trace
    ) {
        String query = retrievalQuery(intent, message, normalized);
        if (query.isBlank()) {
            return List.of();
        }
        try {
            List<KnowledgeSearchResult> results = retrievalService.search(query, ragOptions.maxSnippets());
            if (results.isEmpty()) {
                traceRecorder.add(trace, "Act", "调用 RetrievalService", "知识库未命中：" + query);
            } else {
                traceRecorder.add(trace, "Act", "调用 RetrievalService", "召回 " + results.size() + " 条知识片段："
                        + results.stream().map(KnowledgeSearchResult::title).distinct().limit(3).toList());
            }
            return results;
        } catch (Exception exception) {
            log.warn("Knowledge retrieval failed: query={}, error={}", query, exception.getMessage());
            traceRecorder.add(trace, "Act", "调用 RetrievalService", "知识库检索失败，已继续使用其他工具");
            return List.of();
        }
    }

    private String retrievalQuery(Intent intent, String message, String normalized) {
        return switch (intent) {
            case ESSAY -> "grammar";
            case NEWS, WORD, DAILY_ROUTE, VOCAB_UNFAMILIAR, VOCAB_MASTERED -> "";
            case CHAT -> {
                if (containsAny(normalized, "rag", "retrieval", "retrievalservice", "knowledge", "知识库", "检索", "召回")) {
                    yield "retrieval";
                }
                if (containsAny(normalized, "agent", "tool", "工具", "调用", "loop", "流程")) {
                    yield "agent";
                }
                if (normalized.contains("grammar") || normalized.contains("语法")) {
                    yield "grammar";
                }
                if (normalized.contains("writing") || normalized.contains("essay") || normalized.contains("作文")) {
                    yield "grammar";
                }
                if (normalized.contains("word") || normalized.contains("vocabulary") || normalized.contains("单词")) {
                    yield "vocabulary";
                }
                yield message == null ? "" : message.trim();
            }
        };
    }

    private boolean containsAny(String value, String... keywords) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String localKnowledgeHint(List<KnowledgeSearchResult> snippets) {
        if (snippets == null || snippets.isEmpty()) {
            return "";
        }
        KnowledgeSearchResult first = snippets.get(0);
        return " 参考知识库：" + first.title() + " - " + abbreviate(first.content(), 90);
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private Intent decideIntent(String normalized) {
        if (containsAny(normalized, "今天还有什么没学", "今天没学", "今日任务", "今日进度", "学习路径",
                "what should i learn today", "what is left today")) {
            return Intent.DAILY_ROUTE;
        }
        if (containsAny(normalized, "不熟悉的单词", "不认识的单词", "薄弱单词", "生词", "需要巩固的单词", "需要巩固的词",
                "巩固的单词", "巩固的词", "unfamiliar words", "unknown words", "weak words")) {
            return Intent.VOCAB_UNFAMILIAR;
        }
        if (containsAny(normalized, "已掌握的单词", "掌握了哪些单词", "熟悉的单词", "mastered words")) {
            return Intent.VOCAB_MASTERED;
        }
        if (!lookupWord(normalized).isEmpty()) {
            return Intent.WORD;
        }
        if ((containsAny(normalized, "推荐", "找一篇", "recommend")
                && containsAny(normalized, "news", "新闻", "阅读", "文章", "reading", "read"))) {
            return Intent.NEWS;
        }
        if (containsAny(normalized, "打开写作工坊", "我要批改作文", "开始写作练习")) {
            return Intent.ESSAY;
        }
        return Intent.CHAT;
    }

    private String lookupWord(String message) {
        for (Pattern pattern : WORD_QUERIES) {
            var matcher = pattern.matcher(message.trim());
            if (matcher.matches()) return matcher.group(1).toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private enum Intent {
        DAILY_ROUTE("今日学习路径"),
        VOCAB_UNFAMILIAR("不熟悉词汇归纳"),
        VOCAB_MASTERED("已掌握词汇归纳"),
        WORD("单词查询或背诵"),
        NEWS("英语新闻阅读"),
        ESSAY("作文批改与润色"),
        CHAT("学习问答与上下文追问");

        private final String label;

        Intent(String label) {
            this.label = label;
        }
    }
}
