package com.example.javabigwork;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "linguamate.ai.base-url=http://127.0.0.1:9/v1",
        "linguamate.ai.api-key=test-key",
        "linguamate.ai.chat-model=test-model"
})
@AutoConfigureMockMvc
class JavaBigWorkApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void todayWordsReturnsEightWords() throws Exception {
        mockMvc.perform(get("/api/words/today").param("limit", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(8)))
                .andExpect(jsonPath("$.data[0].word").isNotEmpty())
                .andExpect(jsonPath("$.data[0].levelTag").isNotEmpty());
    }

    @Test
    void dictionaryLookupUsesFastLocalLemma() throws Exception {
        mockMvc.perform(get("/api/dictionary/lookup").param("word", "becomes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.word").value("become"))
                .andExpect(jsonPath("$.data.meaningCn", containsString("变成")))
                .andExpect(jsonPath("$.data.phoneticUs", containsString("/")));
    }

    @Test
    void knowledgeSearchReturnsSeededGrammarChunks() throws Exception {
        mockMvc.perform(post("/api/knowledge/search")
                        .param("query", "grammar")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()", greaterThan(0)))
                .andExpect(content().string(containsString("Common Grammar Errors")));
    }

    @Test
    void chatTraceIncludesRetrievalForGrammarQuestion() throws Exception {
        String body = """
                {
                  "scene": "daily",
                  "message": "How can I improve grammar in writing?"
                }
                """;

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reply").isNotEmpty())
                .andExpect(content().string(containsString("RetrievalService")));
    }

    @Test
    void chatTraceHitsRetrievalForKnowledgeBaseQuestion() throws Exception {
        String body = """
                {
                  "scene": "daily",
                  "message": "retrievalservice 知识库有什么"
                }
                """;

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(content().string(containsString("RetrievalService")))
                .andExpect(content().string(containsString("LinguaMate RAG Knowledge Base")));
    }

    @Test
    void chatTraceHitsRetrievalForChineseKnowledgeBaseQuestion() throws Exception {
        String body = """
                {
                  "scene": "daily",
                  "message": "知识库有什么"
                }
                """;

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(content().string(containsString("RetrievalService")))
                .andExpect(content().string(containsString("LinguaMate RAG Knowledge Base")));
    }

    @Test
    void writingReviewReturnsLocalScoresAndFeedback() throws Exception {
        String body = """
                {
                  "title": "My English Plan",
                  "examType": "general",
                  "targetLevel": "B1",
                  "essayText": "I learn English every day. I read news and write sentence. This habit help me improve."
                }
                """;

        mockMvc.perform(post("/api/writing/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalScore").isNumber())
                .andExpect(jsonPath("$.data.feedback.length()", greaterThan(0)))
                .andExpect(jsonPath("$.data.reviewSource").isNotEmpty());
    }

    @Test
    void newsRecommendationsReturnSeedArticles() throws Exception {
        mockMvc.perform(get("/api/news/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()", greaterThan(0)))
                .andExpect(jsonPath("$.data[0].title").isNotEmpty());
    }
}
