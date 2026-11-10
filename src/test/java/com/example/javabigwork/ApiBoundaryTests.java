package com.example.javabigwork;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.javabigwork.auth.AuthService;
import com.example.javabigwork.auth.LoginActivityService;
import com.example.javabigwork.auth.UserContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "linguamate.ai.base-url=http://127.0.0.1:9/v1",
        "linguamate.ai.api-key=test-key",
        "linguamate.ai.chat-model=test-model",
        "linguamate.cache.redis.enabled=false"
})
@AutoConfigureMockMvc
class ApiBoundaryTests {
    @Autowired private MockMvc mvc;
    @Autowired private UserContext userContext;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void onlyConfiguredFrontendOriginsMayUseCredentialedCors() throws Exception {
        for (String origin : new String[]{"http://localhost:5173", "http://127.0.0.1:5173"}) {
            mvc.perform(options("/api/auth/login")
                            .header(HttpHeaders.ORIGIN, origin)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
        }
        for (String origin : new String[]{"https://unknown.example", "http://localhost:5174", "null"}) {
            mvc.perform(options("/api/auth/login")
                            .header(HttpHeaders.ORIGIN, origin)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
            mvc.perform(post("/api/auth/login").header(HttpHeaders.ORIGIN, origin)
                            .contentType(MediaType.APPLICATION_JSON).content(loginBody()))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void loginRotatesTheSessionAndWorksThroughTheFrontendOrigin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String originalId = session.getId();
        mvc.perform(post("/api/auth/login").session(session)
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(jsonPath("$.data.username").value("student"));
        assertNotEquals(originalId, session.getId());
        mvc.perform(get("/api/report/summary").session(session)
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/report/summary")).andExpect(status().isUnauthorized());
    }

    @Test
    void deniedRequestsDoNotLeaveUserContextOnTheRequestThread() throws Exception {
        MockHttpSession session = login();
        mvc.perform(get("/api/admin/summary").session(session))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.success").value(false));
        assertNull(userContext.currentUser());
        mvc.perform(get("/api/knowledge").session(session)).andExpect(status().isForbidden());
        assertNull(userContext.currentUser());
        mvc.perform(get("/api/words/today").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
        assertNull(userContext.currentUser());
    }

    @Test
    void backendDoesNotServeFrontendRoutesOrAssets() throws Exception {
        for (String path : new String[]{"/", "/login", "/admin", "/mission", "/index.html", "/assets/linguamate-icon.svg"}) {
            mvc.perform(get(path)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
        mvc.perform(get("/api/unknown").session(login())).andExpect(status().isNotFound());
    }

    @Test
    void missionsAreUnavailableByDefaultAndStoredPracticeStaysInGrowthEvidence() throws Exception {
        MockHttpSession session = (MockHttpSession) mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"username":"retiredMission%d","password":"testpass123"}
                                """.formatted(System.nanoTime())))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
        long user = (Long) session.getAttribute(AuthService.SESSION_USER_ID);
        LocalDateTime now = LocalDateTime.now(LoginActivityService.ZONE);
        jdbc.update("""
                INSERT INTO learning_mission_session(user_id, mission_key, study_date, content_json, status, created_at, updated_at)
                VALUES(?, 'water', ?, '{}', 'NEW', ?, ?)
                """, user, now.toLocalDate(), now, now);
        long mission = jdbc.queryForObject("SELECT id FROM learning_mission_session WHERE user_id=?", Long.class, user);
        long word = jdbc.queryForObject("SELECT MIN(id) FROM word", Long.class);
        jdbc.update("""
                INSERT INTO word_review_log(user_id, word_id, review_type, answer, correct, created_at)
                VALUES(?, ?, 'QUIZ', 'stored answer', TRUE, ?)
                """, user, word, now);

        mvc.perform(get("/api/missions/today").session(session)).andExpect(status().isNotFound());
        mvc.perform(get("/api/missions/sessions/" + mission).session(session)).andExpect(status().isNotFound());
        mvc.perform(post("/api/missions/water/start").session(session))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/report/growth").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.missions").isEmpty())
                .andExpect(jsonPath("$.data.words[0].attempts[0].answer").value("stored answer"))
                .andExpect(jsonPath("$.data.writing").isArray())
                .andExpect(jsonPath("$.data.expressions").isArray());
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM learning_mission_session WHERE id=?", Long.class, mission));
    }

    @Test
    void malformedRequestsKeepClientErrorStatus() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(get("/api/auth/login")).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/words/today").param("limit", "invalid").session(login()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readinessRequiresInitializedDatabaseAndDoesNotExposeConnectionDetails() throws Exception {
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.components.readinessState.status").value("UP"))
                .andExpect(jsonPath("$.components.db.details").doesNotExist())
                .andExpect(jsonPath("$.components.redis").doesNotExist());
    }

    private MockHttpSession login() throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody()))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private String loginBody() {
        return "{\"username\":\"student\",\"password\":\"student123\"}";
    }
}
