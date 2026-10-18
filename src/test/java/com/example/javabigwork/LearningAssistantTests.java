package com.example.javabigwork;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {"linguamate.ai.base-url=http://127.0.0.1:9/v1", "linguamate.ai.api-key=test-key",
        "linguamate.ai.chat-model=test-model", "linguamate.cache.redis.enabled=false"})
@AutoConfigureMockMvc
@Transactional
class LearningAssistantTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;

    private HttpServer model;
    private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private final AtomicReference<String> modelReply = new AtomicReference<>("先复习这两个薄弱词，再用它们各造一句话。");

    @BeforeEach
    void startModel() throws Exception {
        model = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        model.createContext("/v1/chat/completions", exchange -> {
            requests.add(json.readTree(exchange.getRequestBody().readAllBytes()));
            byte[] body = json.writeValueAsBytes(Map.of("choices", List.of(Map.of("message", Map.of("content", modelReply.get())))));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        model.start();
    }

    @AfterEach
    void stopModel() { model.stop(0); }

    @Test
    void personalPlanningUsesConfiguredModelActualRecordsAndFollowUpHistory() throws Exception {
        var owner = account();
        var other = account();
        long ownerId = userId(owner);
        long otherId = userId(other);
        seed(ownerId, "assistant-priority", 0, 4, LocalDateTime.now().plusHours(6));
        seed(ownerId, "assistant-due", 2, 0, LocalDateTime.now().minusHours(1));
        seed(otherId, "private-other-learner-word", 0, 99, LocalDateTime.now());
        configure(owner);

        String question = "请根据我的学习记录，找出最近需要巩固的单词，说明推荐理由，并建议我今天先练什么。";
        modelReply.set("先练 assistant-priority：记录中错了 4 次，熟悉度为 0。然后复习已到期的 assistant-due。");
        var first = chat(owner, question, null);
        assertEquals(modelReply.get(), first.path("reply").asText());
        assertEquals("vocabulary-summary", first.path("learningCard").path("type").asText());
        assertEquals("assistant-priority", first.path("learningCard").path("items").get(0).path("label").asText());
        assertFalse(first.path("actions").isEmpty());
        assertEquals("mistakes", first.path("actions").get(0).path("query").path("mode").asText());
        assertTrue(first.path("trace").toString().contains("调用在线模型"));
        assertFalse(first.path("trace").toString().contains("DictionaryTool"));
        assertEquals(question, lastMessage(requests.get(0)));
        var evidence = evidence(requests.get(0));
        assertEquals(2, evidence.path("learningRecords").path("trackedWords").asInt());
        assertEquals(1, evidence.path("learningRecords").path("mistakeWords").asInt());
        assertEquals(1, evidence.path("learningRecords").path("dueWords").asInt());
        assertFalse(evidence.toString().contains("private-other-learner-word"));
        assertFalse(evidence.path("learningRecords").path("priorityWords").get(0).path("due").asBoolean());
        assertEquals("incomplete", evidence.path("learningRecords").path("today").path("items").get(0).path("status").asText());

        String followUp = "我只有五分钟，为什么要先练第一个？请缩短安排。";
        modelReply.set("第一个词错了 4 次，所以优先。两分钟回忆释义，三分钟造句检查。");
        var next = chat(owner, followUp, first.path("conversationId").asLong());
        assertEquals(modelReply.get(), next.path("reply").asText());
        assertEquals(2, requests.size());
        assertEquals("assistant-fixture", requests.get(1).path("model").asText());
        var messages = requests.get(1).path("messages");
        assertEquals(question, messages.get(1).path("content").asText());
        assertEquals(first.path("reply").asText(), messages.get(2).path("content").asText());
        assertEquals(followUp, lastMessage(requests.get(1)));
        assertEquals(2, evidence(requests.get(1)).path("learningRecords").path("trackedWords").asInt());
        assertEquals("incomplete", evidence(requests.get(1)).path("learningRecords").path("today").path("items").get(0).path("status").asText());
    }

    @Test
    void mentionsOfVocabularyReadingAndWritingDoNotHijackTheQuestion() throws Exception {
        var owner = account();
        configure(owner);
        for (String question : List.of("我记单词容易忘，怎么改进？", "阅读和写作该怎样分配时间？",
                "请润色这句话：I want to learn new words through reading.")) {
            modelReply.set("根据你的这条问题：" + question);
            var answer = chat(owner, question, null);
            assertEquals(modelReply.get(), answer.path("reply").asText());
            assertFalse(answer.path("trace").toString().contains("DictionaryTool"));
            assertEquals(question, lastMessage(requests.get(requests.size() - 1)));
        }
        assertEquals(3, requests.size());
        var records = evidence(requests.get(0)).path("learningRecords");
        assertEquals(0, records.path("trackedWords").asInt());
        assertTrue(records.path("priorityWords").isEmpty());
    }

    @Test
    void explicitLookupExtractsTargetInsteadOfTrailingMean() throws Exception {
        var owner = account();
        configure(owner);
        for (String question : List.of("What does perseverance mean?", "解释一下 perseverance", "perseverance是什么意思？")) {
            chat(owner, question, null);
            assertEquals("perseverance", evidence(requests.get(requests.size() - 1)).path("dictionary").path("word").asText());
        }
    }

    @Test
    void modelFailureKeepsRelevantDataAndNeverInventsExampleLookup() throws Exception {
        var owner = account();
        seed(userId(owner), "assistant-local-word", 1, 2, LocalDateTime.now().plusHours(6));
        var answer = chat(owner, "根据学习记录推荐需要巩固的单词，并解释原因", null);
        assertTrue(answer.path("reply").asText().contains("本轮模型暂不可用"));
        assertTrue(answer.path("reply").asText().contains("assistant-local-word"));
        assertTrue(answer.path("reply").asText().contains("错误 2 次"));
        assertFalse(answer.path("reply").asText().contains("example"));
        assertFalse(answer.path("reply").asText().contains("What is one thing"));
        assertTrue(answer.path("trace").toString().contains("使用本地兜底"));
        assertTrue(requests.isEmpty());
    }

    @Test
    void dailyRouteStillCallsModelAndKeepsUsefulNavigation() throws Exception {
        var owner = account();
        seed(userId(owner), "assistant-mistake", 1, 1, LocalDateTime.now().plusDays(1));
        configure(owner);
        var result = chat(owner, "今天还有什么没学？", null);
        assertEquals(1, requests.size());
        assertEquals(modelReply.get(), result.path("reply").asText());
        assertEquals("daily-route", result.path("learningCard").path("type").asText());
        assertEquals("incomplete", result.path("learningCard").path("items").get(0).path("status").asText());
        for (var action : result.path("actions")) {
            if ("/review".equals(action.path("route").asText())) assertEquals("mistakes", action.path("query").path("mode").asText());
        }
    }

    private JsonNode evidence(JsonNode request) throws Exception {
        String prompt = request.path("messages").get(0).path("content").asText();
        String start = "CURRENT_USER_LEARNING_EVIDENCE_JSON:\n";
        return json.readTree(prompt.substring(prompt.indexOf(start) + start.length(), prompt.indexOf("\nEND_LEARNING_EVIDENCE")));
    }

    private String lastMessage(JsonNode request) {
        var messages = request.path("messages");
        return messages.get(messages.size() - 1).path("content").asText();
    }

    private void configure(MockHttpSession owner) throws Exception {
        data(put("/api/ai/settings").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                Map.of("baseUrl", "http://127.0.0.1:" + model.getAddress().getPort() + "/v1",
                        "apiKey", "fixture", "chatModel", "assistant-fixture"))), owner);
    }

    private JsonNode chat(MockHttpSession owner, String message, Long conversation) throws Exception {
        var body = new java.util.HashMap<String, Object>();
        body.put("message", message); body.put("scene", "daily"); body.put("mode", "assistant");
        if (conversation != null) body.put("conversationId", conversation);
        return data(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)), owner);
    }

    private void seed(long user, String word, int familiarity, int wrongCount, LocalDateTime nextReview) {
        db.update("INSERT INTO word(word,meaning_cn,level_tag) VALUES(?,?,'assistant-test')", word, "测试释义");
        Long id = db.queryForObject("SELECT id FROM word WHERE word=?", Long.class, word);
        db.update("INSERT INTO user_word_progress(user_id,word_id,familiarity,wrong_count,review_count,favorite,next_review_at,last_review_at) VALUES(?,?,?,?,1,FALSE,?,?)",
                user, id, familiarity, wrongCount, nextReview, LocalDateTime.now().minusDays(1));
    }

    private long userId(MockHttpSession owner) throws Exception { return data(get("/api/auth/me"), owner).path("id").asLong(); }

    private MockHttpSession account() throws Exception {
        var result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                Map.of("username", "assistant" + System.nanoTime(), "password", "testpass123"))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }

    private JsonNode data(MockHttpServletRequestBuilder request, MockHttpSession owner) throws Exception {
        return json.readTree(mvc.perform(request.session(owner)).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }
}
