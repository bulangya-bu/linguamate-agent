package com.example.javabigwork.agent.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.javabigwork.agent.AgentPromptFactory;
import com.example.javabigwork.agent.AgentTraceRecorder;
import com.example.javabigwork.ai.dto.AiRuntimeSettings;
import com.example.javabigwork.ai.service.AiSettingsService;
import com.example.javabigwork.ai.service.OpenAiCompatibleClient;
import com.example.javabigwork.config.RagConfig.RagRuntimeOptions;
import com.example.javabigwork.knowledge.service.RetrievalService;
import com.example.javabigwork.tool.ReviewPlannerTool;
import com.example.javabigwork.vocabulary.entity.UserWordProgress;
import com.example.javabigwork.vocabulary.entity.Word;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import com.example.javabigwork.chat.entity.Message;
import com.example.javabigwork.ai.dto.OpenAiChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EnglishCoachServiceTests {
    private final AiSettingsService settings = mock(AiSettingsService.class);
    private final OpenAiCompatibleClient client = mock(OpenAiCompatibleClient.class);
    private final ReviewPlannerTool planner = mock(ReviewPlannerTool.class);
    private final WordMapper words = mock(WordMapper.class);
    private final RetrievalService retrieval = mock(RetrievalService.class);
    private EnglishCoachService coach;

    @BeforeEach void setup() {
        coach = new EnglishCoachService(settings, client, new AgentPromptFactory(mock(RagRuntimeOptions.class)),
                new AgentTraceRecorder(), planner, words, retrieval, new ObjectMapper());
        when(settings.resolveCurrent()).thenReturn(new AiRuntimeSettings("openai-compatible", "http://localhost:9/v1", "test-only", "my-coach-model"));
        when(planner.planToday(3)).thenReturn(List.of());
        when(retrieval.search(anyString(), eq(2))).thenReturn(List.of());
    }

    @Test void oneModelCallUsesRealWeakWordsAndReturnsSeparateFeedback() {
        UserWordProgress progress = new UserWordProgress(); progress.wordId = 42L;
        Word word = new Word(); word.id = 42L; word.word = "challenge"; word.meaningCn = "挑战";
        when(planner.planToday(3)).thenReturn(List.of(progress));
        when(words.selectById(42L)).thenReturn(word);
        when(client.chat(any(), anyString(), anyList())).thenReturn("""
                ```json
                {"reply":"That sounds exciting. What was the challenge?", "revisionType":"correction",
                 "improved":"I faced a challenge yesterday.", "explanation":"yesterday 对应过去时。",
                 "nextChallenge":"再说说你如何解决这个挑战。", "phrases":[{"text":"face a challenge","meaningCn":"面对挑战"}]}
                ```
                """);
        var turn = coach.practice("business", "I face a challenge yesterday.", List.of());
        assertEquals("model", turn.coaching().source());
        assertEquals("my-coach-model", turn.coaching().modelName());
        assertEquals("I face a challenge yesterday.", turn.coaching().original());
        assertTrue(turn.coaching().targetWords().get(0).used());
        assertEquals("correction", turn.coaching().revisionType());
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(client, times(1)).chat(any(), prompt.capture(), anyList());
        assertTrue(prompt.getValue().contains("challenge : 挑战"));
        assertTrue(prompt.getValue().contains("workplace English"));
        assertEquals(List.of("Observe", "Think", "Act", "Act", "Act", "Answer"), turn.trace().stream().map(step -> step.phase()).toList());
    }

    @Test void malformedOrUnavailableModelFallsBackWithoutInventedCorrections() {
        when(client.chat(any(), anyString(), anyList())).thenReturn("not valid JSON");
        var turn = coach.practice("exam", "I go yesterday.", List.of());
        assertEquals("local", turn.coaching().source());
        assertEquals("none", turn.coaching().revisionType());
        assertEquals("", turn.coaching().improved());
        assertTrue(turn.coaching().notice().contains("未返回有效反馈"));
        assertTrue(turn.reply().contains("skill"));
        when(client.chat(any(), anyString(), anyList())).thenThrow(new IllegalStateException("offline"));
        assertEquals("local", coach.practice("daily", "Hello", List.of()).coaching().source());
    }

    @Test void chineseSetupIsNotFalselyCorrectedAndUnknownRevisionTypesAreIgnored() {
        when(client.chat(any(), anyString(), anyList())).thenReturn("""
                {"reply":"Tell me about your day.","revisionType":"correction","improved":"Hello there.","explanation":"从日常话题开始。"}
                """);
        var turn = coach.practice("daily", "帮我练习 English", List.of());
        assertEquals("none", turn.coaching().revisionType());
        assertEquals("", turn.coaching().improved());
        assertTrue(turn.coaching().phrases().isEmpty());
    }

    @Test void absentConfigurationDoesNotCallProvider() {
        when(settings.resolveCurrent()).thenReturn(new AiRuntimeSettings("openai-compatible", "", "", ""));
        var turn = coach.practice("reading", "hello", List.of());
        verifyNoInteractions(client);
        assertEquals("local", turn.coaching().source());
        assertTrue(turn.reply().contains("read recently"));
    }

    @Test void nextTurnIncludesPreviousLearningTaskEvenWhenRetrievalFails() {
        Message previous = new Message(); previous.role = "assistant"; previous.content = "What happened?";
        previous.metadata = "{\"coaching\":{\"nextChallenge\":\"Describe the result in the past tense.\"}}";
        when(retrieval.search(anyString(), eq(2))).thenThrow(new IllegalStateException("unavailable"));
        when(client.chat(any(), anyString(), anyList())).thenReturn("{\"reply\":\"That sounds good. What next?\",\"revisionType\":\"none\"}");
        var turn = coach.practice("daily", "It went well.", List.of(previous));
        assertEquals("model", turn.coaching().source());
        @SuppressWarnings("unchecked") ArgumentCaptor<List<OpenAiChatMessage>> history = ArgumentCaptor.forClass(List.class);
        verify(client).chat(any(), anyString(), history.capture());
        assertTrue(history.getValue().get(0).content().contains("Describe the result in the past tense."));
        assertEquals("It went well.", history.getValue().get(1).content());
    }
}
