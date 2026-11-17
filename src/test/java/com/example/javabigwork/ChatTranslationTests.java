package com.example.javabigwork;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.javabigwork.ai.dto.OpenAiChatMessage;
import com.example.javabigwork.ai.service.OpenAiCompatibleClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "linguamate.ai.base-url=http://127.0.0.1:9/v1",
        "linguamate.ai.api-key=",
        "linguamate.ai.chat-model=test-model",
        "linguamate.cache.redis.enabled=false"
})
@AutoConfigureMockMvc
class ChatTranslationTests {
    private static final String REPLY = "That sounds interesting. What did you enjoy most?";
    private static final String TRANSLATION_PROMPT = "Translate the entire English practice reply";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockitoBean private OpenAiCompatibleClient client;

    @Test
    void translatesOnlyTheOwnersSavedPracticeReplyWithoutAddingAMessage() throws Exception {
        MockHttpSession owner = register();
        MockHttpSession other = register();
        JsonNode turn = practice(owner);
        long conversationId = turn.path("conversationId").asLong();
        int replyId = turn.path("messageId").asInt();
        String path = "/api/chat/messages/" + replyId + "/translation";
        var source = List.of(new OpenAiChatMessage("user", REPLY));
        when(client.chat(any(), contains(TRANSLATION_PROMPT), eq(source)))
                .thenReturn("听起来很有趣。你最喜欢哪一点？");

        mvc.perform(post(path)).andExpect(status().isUnauthorized());
        mvc.perform(post(path).session(other)).andExpect(status().isBadRequest());
        mvc.perform(post(path).session(owner)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.translation").value("听起来很有趣。你最喜欢哪一点？"));
        mvc.perform(get("/api/conversations/" + conversationId + "/messages").session(owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].id").value(replyId))
                .andExpect(jsonPath("$.data[1].content").value(REPLY));
        verify(client, times(1)).chat(any(), contains(TRANSLATION_PROMPT), eq(source));
    }

    @Test
    void reportsUnavailableWhenTheProviderReturnsNoChineseTranslation() throws Exception {
        MockHttpSession owner = register();
        int replyId = practice(owner).path("messageId").asInt();
        when(client.chat(any(), contains(TRANSLATION_PROMPT), anyList())).thenReturn(REPLY);

        mvc.perform(post("/api/chat/messages/" + replyId + "/translation").session(owner))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.success").value(false));
    }

    private MockHttpSession register() throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"chatTranslate%d","password":"testpass123"}
                                """.formatted(System.nanoTime())))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private JsonNode practice(MockHttpSession session) throws Exception {
        when(client.chat(any(), contains("adaptive English conversation coach"), anyList()))
                .thenReturn(json.createObjectNode().put("reply", REPLY).toString());
        var response = mvc.perform(post("/api/chat").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scene":"daily","mode":"practice","message":"I visited a museum."}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.messageId").isNumber())
                .andExpect(jsonPath("$.data.reply").value(REPLY))
                .andReturn().getResponse();
        return json.readTree(response.getContentAsByteArray()).path("data");
    }
}
