package com.example.javabigwork;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "linguamate.ai.base-url=http://127.0.0.1:9/v1", "linguamate.ai.api-key=test-key",
        "linguamate.ai.chat-model=test-model", "linguamate.cache.redis.enabled=false"
})
@AutoConfigureMockMvc
class LearningRefinementTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;

    @Test void dueQueueHonorsTimeEvenForPreviouslyMasteredWords() throws Exception {
        MockHttpSession session = account();
        long user = userId(session);
        mvc.perform(post("/api/dictionary/save").param("word","language").session(session)).andExpect(status().isOk());
        assertEquals(1, data(get("/api/reviews/today"), session).size());
        db.update("UPDATE user_word_progress SET next_review_at=? WHERE user_id=?", LocalDateTime.now().plusDays(1), user);
        assertEquals(0, data(get("/api/reviews/today"), session).size());
        assertEquals(1, data(get("/api/reviews/mistakes"), session).size());
        db.update("UPDATE user_word_progress SET wrong_count=0, familiarity=4, next_review_at=? WHERE user_id=?", LocalDateTime.now().minusMinutes(1), user);
        assertEquals(1, data(get("/api/reviews/today"), session).size());
        assertEquals(0, data(get("/api/reviews/mistakes"), session).size());
        assertEquals(1, data(get("/api/report/summary"),session).path("dueWords").asLong());
        assertEquals(0, data(get("/api/reviews/today"), account()).size());
    }

    @Test void readingPreservesActualSentenceAndInflectedTermPerUser() throws Exception {
        MockHttpSession session=account();
        JsonNode article=firstArticle(session);
        String sentence=article.path("content").asText().split("(?<=[.!?])\\s+|\\n")[0].trim();
        String word=sentence.split("[^A-Za-z]+")[0];
        String endpoint="/api/news/"+article.path("id").asLong();
        Map<String,String> body=Map.of("word",word,"sentence",sentence);
        JsonNode fallback=postData(endpoint+"/explain",body,session);
        assertEquals("dictionary",fallback.path("source").asText());
        for(int i=0;i<2;i++) postData(endpoint+"/save-word",body,session);
        JsonNode words=data(get("/api/reviews/today"),session);
        assertEquals(1,words.size());
        assertEquals(sentence,words.path(0).path("sourceSentence").asText());
        assertEquals(word,words.path(0).path("sourceWord").asText());
        assertEquals(article.path("title").asText(),words.path(0).path("sourceTitle").asText());
        mvc.perform(post(endpoint+"/save-word").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("word",word,"sentence","This fabricated sentence is not in the article."))))
                .andExpect(status().isBadRequest());
        assertEquals(0,data(get("/api/reviews/today"),account()).size());
    }

    @Test void revisionLinksAreOwnedAndComparisonUsesSameSettings() throws Exception {
        MockHttpSession session=account();
        JsonNode original=postData("/api/writing/review",essay("I learn English every day. This habit help me improve.",null,"B1"),session);
        long id=original.path("essayId").asLong();
        JsonNode second=postData("/api/writing/review",essay("I learn English every day. This habit helps me improve my vocabulary.",id,"B1"),session);
        assertTrue(second.path("comparison").path("comparable").asBoolean());
        JsonNode changed=postData("/api/writing/review",essay("Learning English gives me new opportunities.",id,"C1"),session);
        assertFalse(changed.path("comparison").path("comparable").asBoolean());
        mvc.perform(post("/api/writing/review").session(account()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(essay("A different essay.",id,"B1")))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/writing/review").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(essay("I learn English every day. This habit help me improve.",id,"B1"))))
                .andExpect(status().isBadRequest());
        assertEquals(2,data(get("/api/report/summary"),session).path("evidence").path("revisedEssays").asLong());
    }

    @Test void evidenceUsesRealStrictReviewSamplesAndKeepsEmptyAccuracyNull() throws Exception {
        MockHttpSession session=account();
        long user=userId(session);
        LocalDateTime recent=LocalDate.now().atTime(1,0), previous=LocalDate.now().minusDays(7).atTime(12,0);
        for(boolean correct: List.of(false,false,true)) log(user,correct,"STRICT_REVIEW",recent);
        for(boolean correct: List.of(false,true)) log(user,correct,"STRICT_REVIEW",previous);
        log(user,true,"QUIZ",recent);
        JsonNode result=data(get("/api/report/summary"),session).path("evidence");
        assertEquals(3,result.path("recentAttempts").asLong());
        assertEquals(33,result.path("recentAccuracy").asLong());
        assertEquals(50,result.path("previousAccuracy").asLong());
        assertEquals(1,result.path("recurringWords").asLong());
        JsonNode other=data(get("/api/report/summary"),account()).path("evidence");
        assertTrue(other.path("recentAccuracy").isNull());
        assertEquals(0,other.path("recentAttempts").asLong());
    }

    @Test void realHttpProtocolConnectsDraftModelAndContextFallsBackHonestly() throws Exception {
        MockHttpSession session=account();
        AtomicReference<String> content=new AtomicReference<>("OK");
        AtomicReference<JsonNode> requestBody=new AtomicReference<>();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/chat/completions",exchange->{
            requestBody.set(json.readTree(exchange.getRequestBody()));
            byte[] bytes=json.writeValueAsBytes(Map.of("choices",List.of(Map.of("message",Map.of("role","assistant","content",content.get())))));
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(200,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            String base="http://127.0.0.1:"+server.getAddress().getPort()+"/v1";
            postData("/api/ai/test",Map.of("baseUrl",base,"apiKey","fixture","chatModel","draft-model"),session);
            assertEquals("draft-model",requestBody.get().path("model").asText());
            assertNotEquals("draft-model",data(get("/api/ai/settings"),session).path("chatModel").asText());
            mvc.perform(put("/api/ai/settings").session(session).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("baseUrl",base,"apiKey","fixture","chatModel","fixture-model")))).andExpect(status().isOk());
            JsonNode article=firstArticle(session);
            String sentence=article.path("content").asText().split("(?<=[.!?])\\s+|\\n")[0].trim();
            String word=sentence.split("[^A-Za-z]+")[0];
            content.set(json.writeValueAsString(Map.of("meaning","本文语境释义","explanation","结合原句说明用法","phrase","invented phrase","phraseMeaning","不得显示")));
            JsonNode context=postData("/api/news/"+article.path("id").asLong()+"/explain",Map.of("word",word,"sentence",sentence),session);
            assertEquals("model",context.path("source").asText()); assertEquals("",context.path("phrase").asText());
            assertTrue(requestBody.get().path("messages").toString().contains(sentence));
            content.set(json.writeValueAsString(Map.of("totalScore",81,"grammarScore",80,"vocabularyScore",82,"structureScore",83,"logicScore",79,
                    "summary","观点清楚，可以改善措辞。","polishedText","I have a book.","feedback",List.of(Map.of("category","grammar","originalSentence","I has a book.","suggestion","主语 I 后使用 have。","correctedSentence","I have a book.")))));
            JsonNode reviewed=postData("/api/writing/review",essay("I has a book.",null,"B1"),session);
            assertEquals("AI批改",reviewed.path("reviewSource").asText()); assertEquals("fixture-model",reviewed.path("modelName").asText());
            assertEquals("I have a book.",reviewed.path("feedback").path(0).path("correctedSentence").asText());
            content.set(json.writeValueAsString(Map.of("reply","What did you learn from this article?", "revisionType","none",
                    "improved","", "explanation","", "phrases",List.of(),"nextChallenge","Explain the main idea.")));
            String userMessage="我想用英语讨论这篇文章。";
            JsonNode chat=postData("/api/chat",Map.of("mode","practice","scene","reading","message",userMessage,
                    "readingArticleId",article.path("id").asLong()),session);
            assertTrue(requestBody.get().path("messages").get(0).path("content").asText().contains(sentence));
            long conversation=chat.path("conversationId").asLong();
            JsonNode history=data(get("/api/conversations/"+conversation+"/messages"),session);
            assertEquals(userMessage,history.path(0).path("content").asText());
            assertEquals(article.path("id").asLong(),json.readTree(history.path(0).path("metadata").asText()).path("readingArticle").path("id").asLong());
            postData("/api/chat",Map.of("mode","practice","scene","reading","conversationId",conversation,"message","I learned a new idea."),session);
            assertTrue(requestBody.get().path("messages").get(0).path("content").asText().contains(sentence));
        } finally { server.stop(0); }
        mvc.perform(post("/api/ai/test").session(session).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("模型暂时无法响应，请检查配置或稍后重试"));
        mvc.perform(post("/api/ai/test").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode firstArticle(MockHttpSession session) throws Exception {
        long id=data(get("/api/news/recommendations"),session).path(0).path("id").asLong();
        return data(get("/api/news/"+id),session);
    }
    private Map<String,Object> essay(String text,Long previous,String level) {
        var result=new java.util.HashMap<String,Object>();
        result.put("title","Learning");result.put("examType","general");result.put("targetLevel",level);result.put("essayText",text);
        if(previous!=null)result.put("revisionOfId",previous);
        return result;
    }
    private void log(long user,boolean correct,String type,LocalDateTime time) {
        db.update("INSERT INTO word_review_log(user_id,word_id,correct,review_type,created_at) VALUES(?,1,?,?,?)",user,correct,type,time);
    }
    private MockHttpSession account() throws Exception {
        String name="refine-"+System.nanoTime();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username",name,"nickname","学习体验测试","password","userpass123")))).andExpect(status().isOk());
        return (MockHttpSession)mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username",name,"password","userpass123"))))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
    private long userId(MockHttpSession session) throws Exception {return data(get("/api/auth/me"),session).path("id").asLong();}
    private JsonNode data(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,MockHttpSession session) throws Exception {
        return json.readTree(mvc.perform(request.session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }
    private JsonNode postData(String path,Object body,MockHttpSession session) throws Exception {
        return data(post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)),session);
    }
}
