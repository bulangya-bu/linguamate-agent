package com.example.javabigwork.agent;

import com.example.javabigwork.ai.service.WebAnswerService;
import com.example.javabigwork.agent.dto.ChatAction;
import com.example.javabigwork.agent.dto.LearningAdvisorView;
import com.example.javabigwork.agent.dto.LearningCard;
import com.example.javabigwork.agent.service.LearningAdvisorService;
import com.example.javabigwork.chat.entity.Message;
import com.example.javabigwork.config.RagConfig.RagRuntimeOptions;
import com.example.javabigwork.knowledge.dto.KnowledgeSearchResult;
import com.example.javabigwork.knowledge.service.RetrievalService;
import com.example.javabigwork.tool.DictionaryTool;
import com.example.javabigwork.tool.NewsRecommendTool;
import java.util.List;
import java.util.Locale;
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

    public AgentOrchestrator(
            AgentTraceRecorder traceRecorder,
            AgentMemoryService memoryService,
            DictionaryTool dictionaryTool,
            NewsRecommendTool newsRecommendTool,
            WebAnswerService webAnswerService,
            RetrievalService retrievalService,
            RagRuntimeOptions ragOptions,
            LearningAdvisorService learningAdvisorService
    ) {
        this.traceRecorder = traceRecorder;
        this.memoryService = memoryService;
        this.dictionaryTool = dictionaryTool;
        this.newsRecommendTool = newsRecommendTool;
        this.webAnswerService = webAnswerService;
        this.retrievalService = retrievalService;
        this.ragOptions = ragOptions;
        this.learningAdvisorService = learningAdvisorService;
    }

    public AgentResult handle(String scene, String message, List<Message> history) {
        log.info("Agent handle: scene={}, intent decision start", scene);
        List<AgentTraceStep> trace = traceRecorder.start(message);
        String normalized = message.toLowerCase(Locale.ROOT);
        Intent intent = decideIntent(normalized);
        log.info("Agent intent: scene={}, intent={}, message={}", scene, intent, message);
        traceRecorder.add(trace, "Think", "判断任务类型", intent.label);

        List<KnowledgeSearchResult> snippets = retrieveKnowledge(intent, message, normalized, trace);

        String reply;
        LearningCard learningCard = null;
        List<ChatAction> actions = List.of();
        if (intent == Intent.DAILY_ROUTE || intent == Intent.VOCAB_UNFAMILIAR || intent == Intent.VOCAB_MASTERED) {
            LearningAdvisorView view = advisorView(intent, trace);
            reply = view.reply();
            learningCard = view.learningCard();
            actions = view.actions();
        } else if (intent == Intent.WORD) {
            String word = extractLastWord(message);
            log.info("Agent tool: DictionaryTool.lookup word={}", word);
            var result = dictionaryTool.lookup(word);
            traceRecorder.add(trace, "Act", "调用 DictionaryTool", "查询单词：" + result.word());
            reply = "单词 " + result.word() + " 的常用释义是：" + result.meaningCn()
                    + "。音标：" + result.phoneticUs() + "。例句：" + result.exampleEn();
        } else if (intent == Intent.NEWS) {
            log.info("Agent tool: NewsRecommendTool.recommend");
            var articles = newsRecommendTool.recommend("easy", "", 2);
            traceRecorder.add(trace, "Act", "调用 NewsRecommendTool", "推荐 easy 难度英语新闻");
            reply = articles.isEmpty()
                    ? "我会为你推荐 easy 难度的英语新闻。当前新闻库还没有数据，可以先导入新闻材料。"
                    : "推荐你先读：" + articles.get(0).title + "。阅读时把鼠标放在单词上，就能查看中文和音标。";
        } else if (intent == Intent.ESSAY) {
            log.info("Agent tool: redirect to essay page");
            traceRecorder.add(trace, "Act", "准备作文批改", "在 Writing 页面提交完整作文后，系统会返回评分、反馈和润色版本。");
            reply = "可以，把作文发到 Writing 页面，我会按语法、词汇、结构和逻辑四个维度批改，并给出润色版本。"
                    + localKnowledgeHint(snippets);
        } else {
            String fallbackReply = "Great. Let's practice in English. Please answer this question: What is one thing you learned today? "
                    + "回答后我会帮你修改表达，并解释可以升级的词汇。"
                    + localKnowledgeHint(snippets);
            reply = webAnswerService.answer(scene, message, history, snippets)
                    .map(answer -> {
                        log.info("Agent LLM call succeeded");
                        traceRecorder.add(trace, "Act", "调用在线模型", "使用当前设置的模型，并注入 RAG 知识片段生成回答");
                        return answer;
                    })
                    .orElseGet(() -> {
                        log.warn("Agent LLM call failed, using fallback");
                        traceRecorder.add(trace, "Act", "使用本地兜底", "模型未配置或调用失败，回退到本地英语练习模板");
                        return fallbackReply;
                    });
        }

        traceRecorder.add(trace, "Answer", "生成回复", reply);
        log.debug("Agent result: replyLen={}, traceSteps={}", reply.length(), trace.size());
        return new AgentResult(reply, trace, memoryService.defaultSuggestions(), learningCard, actions);
    }

    private LearningAdvisorView advisorView(Intent intent, List<AgentTraceStep> trace) {
        try {
            LearningAdvisorView view = switch (intent) {
                case DAILY_ROUTE -> learningAdvisorService.dailyRoute(true);
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
        if (containsAny(normalized, "不熟悉的单词", "不认识的单词", "薄弱单词", "生词", "unfamiliar words",
                "unknown words")) {
            return Intent.VOCAB_UNFAMILIAR;
        }
        if (containsAny(normalized, "已掌握的单词", "掌握了哪些单词", "熟悉的单词", "mastered words")) {
            return Intent.VOCAB_MASTERED;
        }
        if (normalized.contains("word") || normalized.contains("单词") || normalized.contains("释义")) {
            return Intent.WORD;
        }
        if (normalized.contains("news") || normalized.contains("新闻") || normalized.contains("阅读")) {
            return Intent.NEWS;
        }
        if (normalized.contains("essay") || normalized.contains("作文") || normalized.contains("润色")) {
            return Intent.ESSAY;
        }
        return Intent.CHAT;
    }

    private String extractLastWord(String message) {
        String[] parts = message.split("\\s+");
        for (int i = parts.length - 1; i >= 0; i--) {
            String candidate = parts[i].replaceAll("[^A-Za-z'-]", "");
            if (!candidate.isBlank()) {
                return candidate;
            }
        }
        return "example";
    }

    private enum Intent {
        DAILY_ROUTE("今日学习路径"),
        VOCAB_UNFAMILIAR("不熟悉词汇归纳"),
        VOCAB_MASTERED("已掌握词汇归纳"),
        WORD("单词查询或背诵"),
        NEWS("英语新闻阅读"),
        ESSAY("作文批改与润色"),
        CHAT("英语对话练习");

        private final String label;

        Intent(String label) {
            this.label = label;
        }
    }
}
