package com.example.javabigwork;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.javabigwork.auth.LoginActivityService;
import com.example.javabigwork.report.service.LearningJournalService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {
        "linguamate.ai.base-url=http://127.0.0.1:9/v1", "linguamate.ai.api-key=test-key",
        "linguamate.ai.chat-model=test-model", "linguamate.cache.redis.enabled=false"
})
@AutoConfigureMockMvc
class LearningJournalTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    private final LocalDate today=LocalDate.now(LoginActivityService.ZONE);

    @Test void journalUsesDistinctWordsActualDatesAndOwnedRecords() throws Exception {
        String name="journal"+System.nanoTime();
        MockHttpSession session=account(name);
        long user=userId(session);
        jdbc.update("UPDATE user_account SET created_at=? WHERE id=?",today.minusDays(40).atStartOfDay(),user);
        log(user,1,"QUIZ",false,today.minusDays(5));
        log(user,1,"QUIZ",true,today.minusDays(1));
        for(int i=0;i<2;i++){log(user,1,"QUIZ",true,today);log(user,2,"QUIZ",true,today);}
        log(user,1,"STRICT_REVIEW",false,today);log(user,1,"STRICT_REVIEW",true,today);log(user,3,"STRICT_REVIEW",true,today);
        jdbc.update("INSERT INTO essay_submission(user_id,title,original_text,total_score,created_at) VALUES(?, 'Journal test', 'An essay.', 75, ?)",user,today.minusDays(3).atTime(12,0));
        jdbc.update("INSERT INTO reading_record(user_id,article_id,read_progress,finished,created_at,updated_at) VALUES(?,1,100,TRUE,?,?)",user,today.minusDays(2).atTime(12,0),today.minusDays(2).atTime(12,0));
        JsonNode result=journal(session);
        assertEquals(84,result.path("days").size());
        assertEquals(41,result.path("membershipDays").asInt());
        assertEquals(1,result.path("loginDays").asInt());
        assertEquals(today.toString(),result.path("loginRecordedSince").asText());
        assertEquals(3,result.path("totals").path("studiedWords").asInt());
        assertEquals(2,result.path("totals").path("reviewWords").asInt());
        assertEquals(6,result.path("totals").path("quizAttempts").asInt());
        assertEquals(3,result.path("totals").path("reviewAttempts").asInt());
        assertEquals(67,result.path("totals").path("reviewAccuracy").asInt());
        assertEquals(1,result.path("totals").path("essays").asInt());
        assertEquals(1,result.path("totals").path("readings").asInt());
        assertEquals(5,result.path("activeDays").asInt());
        JsonNode current=day(result,today);
        assertEquals(2,current.path("newWords").asInt());
        assertEquals(2,current.path("reviewWords").asInt());
        assertEquals(67,current.path("reviewAccuracy").asInt());
        JsonNode other=journal(account("journalOther"+System.nanoTime()));
        assertEquals(0,other.path("totals").path("studiedWords").asInt());
        assertTrue(other.path("totals").path("reviewAccuracy").isNull());
        assertEquals(0,other.path("activeDays").asInt());
    }

    @Test void successfulVisitsAreDeduplicatedAndSessionRolloverIsRecorded() throws Exception {
        String name="journalLogin"+System.nanoTime();MockHttpSession session=account(name);long user=userId(session);
        for(int i=0;i<3;i++)journal(session);
        assertEquals(1,journal(session).path("loginDays").asInt());
        jdbc.update("INSERT INTO user_login_day(user_id,login_date,first_seen_at) VALUES(?,?,?)",user,today.minusDays(1),today.minusDays(1).atTime(12,0));
        session.setAttribute("LINGUAMATE_RECORDED_LOGIN_DAY",user+":"+today.minusDays(1));
        JsonNode result=journal(session);
        assertEquals(2,result.path("loginDays").asInt());assertEquals(2,result.path("currentStreak").asInt());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username",name,"password","wrong-pass")))).andExpect(status().isBadRequest());
        assertEquals(2,journal(session).path("loginDays").asInt());
        mvc.perform(get("/api/report/journal")).andExpect(status().isUnauthorized());
        mvc.perform(get("/report")).andExpect(status().isNotFound());
    }

    @Test void dailyTasksCountOwnedModelResultsByDate() throws Exception {
        MockHttpSession session = account("journalAi" + System.nanoTime());
        MockHttpSession other = account("journalOtherAi" + System.nanoTime());
        long user = userId(session);
        long otherUser = userId(other);
        long conversation = conversation(user);
        long otherConversation = conversation(otherUser);
        String modelFeedback = """
                {"coaching":{"source":"model"}}
                """;
        message(conversation, "assistant", modelFeedback, today.atStartOfDay());
        message(conversation, "assistant", modelFeedback, today.atStartOfDay().minusSeconds(1));
        message(conversation, "assistant", "{\"coaching\":{\"source\":\"fallback\"}}", today.atTime(12, 0));
        message(conversation, "assistant", "{\"coaching\":null}", today.atTime(12, 0));
        message(conversation, "assistant", "legacy metadata", today.atTime(12, 0));
        message(conversation, "user", modelFeedback, today.atTime(12, 0));
        message(otherConversation, "assistant", modelFeedback, today.atTime(12, 0));
        essay(user, "AI批改", today.atTime(12, 0));
        essay(user, "AI批改", today.minusDays(1).atTime(12, 0));
        essay(user, "本地兜底", today.atTime(12, 0));
        essay(user, null, today.atTime(12, 0));
        essay(otherUser, "AI批改", today.atTime(12, 0));

        JsonNode result = journal(session);
        JsonNode current = day(result, today);
        assertEquals(1, current.required("aiChatTurns").asInt());
        assertEquals(1, current.required("aiEssayReviews").asInt());
        JsonNode yesterday = day(result, today.minusDays(1));
        assertEquals(1, yesterday.required("aiChatTurns").asInt());
        assertEquals(1, yesterday.required("aiEssayReviews").asInt());
    }

    @Test void dailyReadingRecordsTheFirstCompletionWithoutInferringLegacyDates() throws Exception {
        MockHttpSession session = account("journalReading" + System.nanoTime());
        long user = userId(session);
        List<Long> articles = jdbc.queryForList("SELECT id FROM news_article ORDER BY id LIMIT 3", Long.class);
        long automatic = articles.get(0);
        long manual = articles.get(1);
        long legacy = articles.get(2);
        jdbc.update("""
                INSERT INTO reading_record(user_id,article_id,read_progress,finished,created_at,updated_at)
                VALUES(?,?,100,TRUE,?,?)
                """, user, legacy, today.minusDays(1).atTime(12, 0), today.atTime(12, 0));
        mvc.perform(get("/api/news/" + automatic).session(session)).andExpect(status().isOk());
        readingProgress(session, automatic, 89, false).andExpect(jsonPath("$.data.finished").value(false));
        assertEquals(0, day(journal(session), today).required("readingsFinished").asInt());

        readingProgress(session, automatic, 90, false).andExpect(jsonPath("$.data.finished").value(true));
        readingProgress(session, manual, 20, true).andExpect(jsonPath("$.data.readProgress").value(100));
        assertEquals(2, day(journal(session), today).required("readingsFinished").asInt());
        jdbc.update("UPDATE reading_record SET finished_at=? WHERE user_id=? AND article_id=?",
                today.minusDays(1).atTime(12, 0), user, automatic);
        readingProgress(session, automatic, 100, true).andExpect(jsonPath("$.data.finished").value(true));
        readingProgress(session, legacy, 100, true).andExpect(jsonPath("$.data.finished").value(true));

        JsonNode result = journal(session);
        assertEquals(1, day(result, today).required("readingsFinished").asInt());
        assertEquals(1, day(result, today.minusDays(1)).required("readingsFinished").asInt());
    }

    @Test void streaksHandleGapsUnsortedDatesAndYearBoundaries() {
        LocalDate now=LocalDate.of(2026,1,2);
        Set<LocalDate> dates=Set.of(now,now.minusDays(1),now.minusDays(2),now.minusDays(3));
        assertEquals(4,LearningJournalService.streak(dates,now));
        assertEquals(2,LearningJournalService.streak(Set.of(now.minusDays(1),now.minusDays(2)),now));
        assertEquals(0,LearningJournalService.streak(Set.of(now.minusDays(2)),now));
        assertEquals(3,LearningJournalService.longestStreak(List.of(now,now.minusDays(5),now.minusDays(1),now.minusDays(2),now.minusDays(1))));
    }

    private void log(long user,long word,String type,boolean correct,LocalDate date){jdbc.update("INSERT INTO word_review_log(user_id,word_id,review_type,correct,created_at) VALUES(?,?,?,?,?)",user,word,type,correct,date.atTime(12,0));}
    private long conversation(long user) {
        jdbc.update("INSERT INTO conversation(user_id, title) VALUES(?, 'Daily task test')", user);
        return jdbc.queryForObject("SELECT id FROM conversation WHERE user_id=?", Long.class, user);
    }
    private void message(long conversation, String role, String metadata, LocalDateTime date) {
        jdbc.update("INSERT INTO message(conversation_id, role, metadata, created_at) VALUES(?,?,?,?)",
                conversation, role, metadata, date);
    }
    private void essay(long user, String source, LocalDateTime date) {
        jdbc.update("""
                INSERT INTO essay_submission(user_id, title, original_text, review_source, created_at)
                VALUES(?, 'Daily task test', 'An essay.', ?, ?)
                """, user, source, date);
    }
    private ResultActions readingProgress(
            MockHttpSession session, long article, int progress, boolean finished) throws Exception {
        return mvc.perform(post("/api/news/" + article + "/progress").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("readProgress", progress, "finished", finished))))
                .andExpect(status().isOk());
    }
    private JsonNode day(JsonNode response,LocalDate date){for(JsonNode day:response.path("days"))if(day.path("date").asText().equals(date.toString()))return day;throw new AssertionError("Missing date");}
    private MockHttpSession account(String name)throws Exception {
        var result=mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username",name,"nickname","手账测试","password","userpass123"))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession)result.getRequest().getSession(false);
    }
    private long userId(MockHttpSession session)throws Exception{return json.readTree(mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data").path("id").asLong();}
    private JsonNode journal(MockHttpSession session)throws Exception{return json.readTree(mvc.perform(get("/api/report/journal").session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");}
}
