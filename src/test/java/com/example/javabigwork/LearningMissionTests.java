package com.example.javabigwork;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties={"linguamate.ai.base-url=http://127.0.0.1:9/v1","linguamate.ai.api-key=test-key","linguamate.ai.chat-model=test-model","linguamate.cache.redis.enabled=false"})
@AutoConfigureMockMvc
class LearningMissionTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Test void missionsAreDailyResumableAndOwned() throws Exception {
        mvc.perform(get("/api/missions/today")).andExpect(status().isUnauthorized());
        var owner=account();var other=account();
        assertEquals(2,getData("/api/missions/today",owner).size());
        var first=postData("/api/missions/water/start",Map.of(),owner);
        assertEquals(first.path("id"),postData("/api/missions/water/start",Map.of(),owner).path("id"));
        var read=mutate(first,"read",Map.of(),owner);
        assertTrue(read.path("progress").path("readingAt").isTextual());
        assertEquals(read,getData(path(first),owner));
        mvc.perform(get(path(first)).session(other)).andExpect(status().isBadRequest());
        bad(path(first)+"/read",Map.of("version",1),other);
        assertEquals(0,getData("/api/missions/history",other).size());
    }
    @Test void orderedProgressAndVersionPreventDuplicateVocabularyRecords() throws Exception {
        var owner=account();var task=postData("/api/missions/water/start",Map.of(),owner);
        bad(path(task)+"/finish",Map.of("version",0,"reflection","还没有开始，不应完成"),owner);
        bad(path(task)+"/hint",Map.of("version",0,"step",0),owner);
        task=mutate(task,"read",Map.of(),owner);
        bad(path(task)+"/words",Map.of("version",0,"index",0,"answer","减少"),owner);
        task=mutate(task,"words",Map.of("index",0,"answer","借用"),owner);
        assertFalse(task.path("progress").path("words").get(0).path("correct").asBoolean());
        long word=task.path("progress").path("words").get(0).path("wordId").asLong();long user=userId(owner);
        assertEquals("借用",db.queryForObject("SELECT answer FROM word_review_log WHERE user_id=? AND word_id=? ORDER BY id DESC LIMIT 1",String.class,user,word));
        assertEquals(1,db.queryForObject("SELECT wrong_count FROM user_word_progress WHERE user_id=? AND word_id=?",Integer.class,user,word));
        bad(path(task)+"/words",Map.of("version",task.path("version").asInt(),"index",0,"answer","减少"),owner);
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM word_review_log WHERE user_id=?",Integer.class,user));
        bad(path(task)+"/turns",Map.of("version",task.path("version").asInt(),"step",0,"answer","We want to reduce water waste."),owner);
    }
    @Test void realAnswersHintUseAndRetriesSurviveCompletion() throws Exception {
        var owner=account();var task=ready("water",owner);
        task=mutate(task,"hint",Map.of("step",0),owner);
        task=mutate(task,"turns",Map.of("step",0,"answer","We want to reduce water waste."),owner);
        var first=task.path("progress").path("turns").get(0);
        assertEquals(1,first.path("hintLevel").asInt());
        assertEquals("local",first.path("feedback").path("source").asText());
        assertTrue(first.path("coverage").get(0).path("present").asBoolean());
        assertTrue(first.path("trace").size()>=4);
        task=mutate(task,"turns",Map.of("step",0,"answer","The team wants to reduce water waste because some taps keep running."),owner);
        assertEquals(2,task.path("progress").path("turns").size());
        assertEquals(1,task.path("progress").path("turns").get(1).path("hintLevel").asInt());
        task=mutate(task,"turns",Map.of("step",1,"answer","The sensor detects a hand and water flows."),owner);
        task=mutate(task,"turns",Map.of("step",2,"answer","I would report the leaking tap to the team."),owner);
        task=mutate(task,"finish",Map.of("reflection","我学会了用自己的话说明传感器的工作过程。"),owner);
        assertEquals("COMPLETED",task.path("status").asText());assertTrue(task.path("completedAt").isTextual());
        bad(path(task)+"/turns",Map.of("version",task.path("version").asInt(),"step",0,"answer","We need to save water every day."),owner);
        var evidence=getData("/api/report/growth",owner);
        assertEquals(1,evidence.path("missions").size());assertEquals(3,evidence.path("words").size());
        assertEquals(0,getData("/api/report/growth",account()).path("missions").size());
    }
    @Test void welcomeHasDifferentMaterialAndHintsAreProgressiveAndCapped() throws Exception {
        var owner=account();var task=ready("welcome",owner);
        bad(path(task)+"/hint",Map.of("version",task.path("version").asInt(),"step",2),owner);
        for(int i=0;i<4;i++)task=mutate(task,"hint",Map.of("step",0),owner);
        assertEquals(3,task.path("progress").path("hints").get(0).asInt());
        assertTrue(task.path("lesson").path("role").asText().contains("Jamie"));
        bad(path(task)+"/turns",Map.of("version",task.path("version").asInt(),"step",0,"answer","你好你好你好你好你好"),owner);
        task=mutate(task,"turns",Map.of("step",0,"answer","Walk straight past the garden and turn left at the student centre."),owner);
        assertEquals("straight",task.path("progress").path("turns").get(0).path("coverage").get(0).path("evidence").get(0).asText());
    }
    @Test void dueAndMistakeGroupsUseDistinctConsistentDefinitions() throws Exception {
        var owner=account();long user=userId(owner);
        db.update("INSERT INTO user_word_progress(user_id,word_id,familiarity,wrong_count,review_count,favorite,next_review_at) VALUES(?,1,4,0,1,FALSE,?)",user,LocalDateTime.now().minusHours(1));
        db.update("INSERT INTO user_word_progress(user_id,word_id,familiarity,wrong_count,review_count,favorite,next_review_at) VALUES(?,2,1,2,1,FALSE,?)",user,LocalDateTime.now().plusHours(6));
        var stats=getData("/api/word-bank/stats",owner);
        assertEquals(1,stats.path("due").asInt());assertEquals(1,stats.path("review").asInt());
        assertEquals(1,getData("/api/word-bank/words?scope=due",owner).path("total").asInt());
        assertEquals(stats.path("due").asLong(),getData("/api/report/summary",owner).path("dueWords").asLong());
    }
    @Test void growthKeepsTheActualChatAnswerAndDoesNotExposeAnotherAccount() throws Exception {
        var owner=account();var other=account();String original="I want to explain how our campus saves water.";
        postData("/api/chat",Map.of("message",original,"scene","daily","mode","practice"),owner);
        var records=getData("/api/report/growth",owner).path("expressions");
        assertEquals(1,records.size());assertEquals(original,records.get(0).path("answer").asText());
        assertEquals("local",records.get(0).path("feedback").path("source").asText());
        assertEquals(0,getData("/api/report/growth",other).path("expressions").size());
    }
    @Test void curatedExamplesReplaceOnlyGenericPlaceholders() {
        var word=new com.example.javabigwork.vocabulary.entity.Word();word.word="accept";
        word.exampleEn="The word \"accept\" is useful in English practice.";
        word.exampleCn="单词“accept”在英语练习中很有用。";
        assertTrue(com.example.javabigwork.vocabulary.service.WordExampleFallback.english(word).contains("accepted our invitation"));
        assertTrue(com.example.javabigwork.vocabulary.service.WordExampleFallback.chinese(word).contains("邀请"));
        word.exampleEn="I accept your idea.";
        assertEquals("I accept your idea.",com.example.javabigwork.vocabulary.service.WordExampleFallback.english(word));
    }
    @Test void aNewDayStartsFreshWithoutLosingEarlierEvidence() throws Exception {
        var owner=account();var previous=postData("/api/missions/water/start",Map.of(),owner);
        previous=mutate(previous,"read",Map.of(),owner);
        db.update("UPDATE learning_mission_session SET study_date=? WHERE id=?",java.time.LocalDate.now(com.example.javabigwork.auth.LoginActivityService.ZONE).minusDays(1),previous.path("id").asLong());
        assertTrue(getData("/api/missions/today",owner).get(0).path("id").isNull());
        var fresh=postData("/api/missions/water/start",Map.of(),owner);
        assertNotEquals(previous.path("id"),fresh.path("id"));assertTrue(fresh.path("progress").path("readingAt").isNull());
        assertTrue(getData(path(previous),owner).path("progress").path("readingAt").isTextual());
        assertEquals(2,getData("/api/report/growth",owner).path("missions").size());
    }
    @Test void writingEvidenceUsesLearnerSubmissionsInsteadOfTheSuggestedRewrite() throws Exception {
        var owner=account();String before="I learn English every day. This habit help me improve.";
        String after="I learn English every day. This habit helps me improve my vocabulary.";
        var original=postData("/api/writing/review",Map.of("title","Learning","examType","general","targetLevel","B1","essayText",before),owner);
        postData("/api/writing/review",Map.of("title","Learning","examType","general","targetLevel","B1","essayText",after,"revisionOfId",original.path("essayId").asLong()),owner);
        var record=getData("/api/report/growth",owner).path("writing").get(0);
        assertEquals(before,record.path("before").asText());assertEquals(after,record.path("after").asText());
        assertTrue(record.path("comparable").asBoolean());
        assertEquals(0,getData("/api/report/growth",account()).path("writing").size());
    }
    @Test void configuredModelReceivesTheActualMissionQuestionAndViewedHints() throws Exception {
        var owner=account();var task=ready("water",owner);task=mutate(task,"hint",Map.of("step",0),owner);
        var request=new java.util.concurrent.atomic.AtomicReference<JsonNode>();
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/chat/completions",exchange->{
            request.set(json.readTree(exchange.getRequestBody().readAllBytes()));
            String feedback=json.writeValueAsString(Map.of("reply","How could checking taps help?","revisionType","upgrade",
                "improved","Our team aims to reduce water waste on campus.","explanation","用 aims to 说明项目目标。",
                "nextChallenge","Add one reason for saving water.","phrases",java.util.List.of(Map.of("text","reduce water waste","meaningCn","减少水资源浪费"))));
            byte[] response=json.writeValueAsBytes(Map.of("choices",java.util.List.of(Map.of("message",Map.of("content",feedback)))));
            exchange.getResponseHeaders().add("Content-Type","application/json");exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);exchange.close();
        });server.start();
        try {
            String base="http://127.0.0.1:"+server.getAddress().getPort()+"/v1";
            data(put("/api/ai/settings").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("baseUrl",base,"apiKey","fixture","chatModel","mission-fixture"))),owner);
            task=mutate(task,"turns",Map.of("step",0,"answer","Our team wants to reduce water waste."),owner);
            var turn=task.path("progress").path("turns").get(0);
            assertEquals("model",turn.path("feedback").path("source").asText());
            assertEquals("mission-fixture",request.get().path("model").asText());
            String prompt=request.get().path("messages").get(0).path("content").asText();
            assertTrue(prompt.contains(task.path("lesson").path("steps").get(0).path("question").asText()));
            assertTrue(prompt.contains("试着带上 reduce / waste / water。"));
            assertTrue(turn.path("feedback").path("targetWords").get(0).path("used").asBoolean());
            assertEquals("Our team wants to reduce water waste.",getData("/api/report/growth",owner).path("missions").get(0).path("progress").path("turns").get(0).path("answer").asText());
        } finally {server.stop(0);}
    }
    private JsonNode ready(String key,MockHttpSession session)throws Exception {
        var task=postData("/api/missions/"+key+"/start",Map.of(),session);task=mutate(task,"read",Map.of(),session);
        for(int index=0;index<3;index++)task=mutate(task,"words",Map.of("index",index,"answer",task.path("lesson").path("words").get(index).path("meaning").asText()),session);
        return task;
    }
    private JsonNode mutate(JsonNode task,String action,Map<String,Object> fields,MockHttpSession session)throws Exception {
        var body=new java.util.HashMap<>(fields);body.put("version",task.path("version").asInt());return postData(path(task)+"/"+action,body,session);
    }
    private String path(JsonNode task){return "/api/missions/sessions/"+task.path("id").asLong();}
    private long userId(MockHttpSession session)throws Exception{return getData("/api/auth/me",session).path("id").asLong();}
    private MockHttpSession account()throws Exception {
        var result=mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username","mission"+System.nanoTime(),"password","testpass123"))))
            .andExpect(status().isOk()).andReturn();return (MockHttpSession)result.getRequest().getSession();
    }
    private JsonNode getData(String path,MockHttpSession session)throws Exception{return data(get(path),session);}
    private JsonNode postData(String path,Object body,MockHttpSession session)throws Exception{return data(post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)),session);}
    private JsonNode data(MockHttpServletRequestBuilder request,MockHttpSession session)throws Exception {
        return json.readTree(mvc.perform(request.session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }
    private void bad(String path,Object body,MockHttpSession session)throws Exception{mvc.perform(post(path).session(session).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isBadRequest());}
}
