package com.example.javabigwork;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "linguamate.ai.base-url=http://127.0.0.1:9/v1",
        "linguamate.ai.api-key=test-key",
        "linguamate.ai.chat-model=test-model",
        "linguamate.cache.redis.enabled=false"
})
@AutoConfigureMockMvc
class JavaBigWorkApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void todayWordsReturnsEightWords() throws Exception {
        mockMvc.perform(get("/api/words/today").param("limit", "8").session(studentSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(8)))
                .andExpect(jsonPath("$.data[0].word").isNotEmpty())
                .andExpect(jsonPath("$.data[0].levelTag").isNotEmpty())
                .andExpect(jsonPath("$.data[0].exampleEn").isNotEmpty());
    }

    @Test
    void wordBankCategoriesIncludeExamBanks() throws Exception {
        mockMvc.perform(get("/api/word-bank/categories").session(studentSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(content().string(containsString("cet4-bank")))
                .andExpect(content().string(containsString("cet6-bank")))
                .andExpect(content().string(containsString("kaoyan-bank")))
                .andExpect(content().string(containsString("ielts-bank")));
    }

    @Test
    void todayWordsCanFilterByExamCategory() throws Exception {
        mockMvc.perform(get("/api/words/today")
                        .param("limit", "8")
                        .param("category", "kaoyan-bank")
                        .session(studentSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(8)))
                .andExpect(jsonPath("$.data[*].levelTag").value(
                        org.hamcrest.Matchers.everyItem(equalTo("kaoyan-bank"))));

        mockMvc.perform(get("/api/words/today")
                        .param("limit", "8")
                        .param("category", "ielts-bank")
                        .session(studentSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(8)))
                .andExpect(jsonPath("$.data[*].levelTag").value(
                        org.hamcrest.Matchers.everyItem(equalTo("ielts-bank"))));
    }

    @Test
    void todayWordsUnknownCategoryReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/words/today")
                        .param("limit", "8")
                        .param("category", "missing-bank")
                        .session(studentSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void quizResultReturnsExampleSentence() throws Exception {
        String body = """
                {
                  "answer": "not-the-answer"
                }
                """;

        mockMvc.perform(post("/api/words/1/quiz")
                        .session(studentSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.expectedMeaning").isNotEmpty())
                .andExpect(jsonPath("$.data.exampleEn").isNotEmpty());
    }

    @Test
    void strictReviewPassesOnlyWhenSpellingAndMeaningAreCorrect() throws Exception {
        ReviewWord reviewWord = seedWrongReviewWord();

        mockMvc.perform(post("/api/reviews/" + reviewWord.wordId())
                        .session(reviewWord.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewBody(reviewWord.word(), reviewWord.meaningCn())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.spellingCorrect").value(true))
                .andExpect(jsonPath("$.data.meaningCorrect").value(true))
                .andExpect(jsonPath("$.data.expectedWord").value(reviewWord.word()))
                .andExpect(jsonPath("$.data.wrongCount").value(0))
                .andExpect(jsonPath("$.data.familiarity").value(greaterThan(2)));

        MvcResult mastered = mockMvc.perform(get("/api/reviews/mastered")
                        .param("limit", "20")
                        .session(reviewWord.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        assertTrue(responseContainsWord(mastered, reviewWord.wordId()));
    }

    @Test
    void strictReviewRejectsPartialAnswersAndCountsEveryWrongAttempt() throws Exception {
        ReviewWord reviewWord = seedWrongReviewWord();

        mockMvc.perform(post("/api/reviews/" + reviewWord.wordId())
                        .session(reviewWord.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewBody(reviewWord.word(), "not-the-meaning")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.spellingCorrect").value(true))
                .andExpect(jsonPath("$.data.meaningCorrect").value(false))
                .andExpect(jsonPath("$.data.wrongCount").value(2));

        mockMvc.perform(post("/api/reviews/" + reviewWord.wordId())
                        .session(reviewWord.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewBody("bad-spelling", reviewWord.meaningCn())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.spellingCorrect").value(false))
                .andExpect(jsonPath("$.data.meaningCorrect").value(true))
                .andExpect(jsonPath("$.data.wrongCount").value(3));
    }

    @Test
    void todayReviewsReturnWrongWordsOnlyAndRemovePassedWord() throws Exception {
        ReviewWord reviewWord = seedWrongReviewWord();

        MvcResult before = mockMvc.perform(get("/api/reviews/today")
                        .param("limit", "8")
                        .session(reviewWord.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        assertTrue(responseContainsWord(before, reviewWord.wordId()));

        mockMvc.perform(post("/api/reviews/" + reviewWord.wordId())
                        .session(reviewWord.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewBody(reviewWord.word(), reviewWord.meaningCn())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(true));

        MvcResult after = mockMvc.perform(get("/api/reviews/today")
                        .param("limit", "8")
                        .session(reviewWord.session()))
                .andExpect(status().isOk())
                .andReturn();
        assertFalse(responseContainsWord(after, reviewWord.wordId()));
    }

    @Test
    void dictionaryLookupUsesFastLocalLemma() throws Exception {
        mockMvc.perform(get("/api/dictionary/lookup").param("word", "becomes").session(studentSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.word").value("become"))
                .andExpect(jsonPath("$.data.meaningCn", containsString("变成")))
                .andExpect(jsonPath("$.data.phoneticUs", containsString("/")));
    }

    @Test
    void wordBankSearchMatchesEnglishPrefixOnly() throws Exception {
        mockMvc.perform(get("/api/word-bank/words")
                        .session(studentSession())
                        .param("search", "AP")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total", greaterThan(0)))
                .andExpect(jsonPath("$.data.words[*].word", not(hasSize(0))))
                .andExpect(jsonPath("$.data.words[*].word").value(
                        org.hamcrest.Matchers.everyItem(matchesPattern("(?i)^ap.*"))));
    }

    @Test
    void wordBankSearchMatchesChineseMeaningPrefixOnly() throws Exception {
        mockMvc.perform(get("/api/word-bank/words")
                        .session(studentSession())
                        .param("search", "变")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total", greaterThan(0)))
                .andExpect(jsonPath("$.data.words[*].meaningCn").value(
                        org.hamcrest.Matchers.everyItem(matchesPattern("^变.*"))));

        mockMvc.perform(get("/api/word-bank/words")
                        .session(studentSession())
                        .param("search", "词语")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void wordBankSearchWorksWithCategoryAndPagination() throws Exception {
        mockMvc.perform(get("/api/word-bank/words")
                        .session(studentSession())
                        .param("category", "local-fast")
                        .param("search", "a")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.words[*].levelTag").value(
                        org.hamcrest.Matchers.everyItem(equalTo("local-fast"))))
                .andExpect(jsonPath("$.data.words[*].word").value(
                        org.hamcrest.Matchers.everyItem(matchesPattern("(?i)^a.*"))));
    }

    @Test
    void knowledgeSearchReturnsSeededGrammarChunks() throws Exception {
        mockMvc.perform(post("/api/knowledge/search")
                        .session(adminSession())
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
                        .session(studentSession())
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
                        .session(studentSession())
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
                        .session(studentSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(content().string(containsString("RetrievalService")))
                .andExpect(content().string(containsString("LinguaMate RAG Knowledge Base")));
    }

    @Test
    void chatRecommendsTodaysIncompleteLearningRouteWithActions() throws Exception {
        String username = "route" + System.nanoTime();
        registerUser(username, "Route User");
        MockHttpSession session = login(username, "userpass123");

        mockMvc.perform(post("/api/chat")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scene": "daily",
                                  "message": "今天还有什么没学的？"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learningCard.type").value("daily-route"))
                .andExpect(jsonPath("$.data.learningCard.items", hasSize(5)))
                .andExpect(jsonPath("$.data.actions.length()", greaterThan(0)))
                .andExpect(jsonPath("$.data.actions[*].route",
                        org.hamcrest.Matchers.everyItem(matchesPattern("^/(chat|vocabulary|review|writing|news)$"))));
    }

    @Test
    void chatAndEndpointSummarizeUnfamiliarVocabulary() throws Exception {
        String username = "vocab" + System.nanoTime();
        registerUser(username, "Vocab User");
        MockHttpSession session = login(username, "userpass123");
        MvcResult words = mockMvc.perform(get("/api/words/today").param("limit", "1").session(session))
                .andExpect(status().isOk())
                .andReturn();
        long wordId = objectMapper.readTree(words.getResponse().getContentAsString())
                .path("data").path(0).path("id").asLong();
        mockMvc.perform(post("/api/words/" + wordId + "/quiz")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"wrong-answer\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/chat")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scene": "daily",
                                  "message": "归纳一下我所有不熟悉的单词"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learningCard.type").value("vocabulary-summary"))
                .andExpect(jsonPath("$.data.learningCard.group").value("unfamiliar"))
                .andExpect(jsonPath("$.data.learningCard.totalElements", greaterThan(0)));

        mockMvc.perform(get("/api/learning-advisor/vocabulary")
                        .session(session)
                        .param("group", "unfamiliar")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learningCard.group").value("unfamiliar"));
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
                        .session(studentSession())
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
        mockMvc.perform(get("/api/news/recommendations").session(studentSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()", greaterThan(0)))
                .andExpect(jsonPath("$.data[0].title").isNotEmpty());
    }

    @Test
    void newsTopicsAndTopicFilterUseArticleTags() throws Exception {
        MockHttpSession session = studentSession();

        mockMvc.perform(get("/api/news/topics").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(content().string(containsString("story")));

        mockMvc.perform(get("/api/news/recommendations")
                        .param("topic", "story")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()", greaterThan(0)))
                .andExpect(jsonPath("$.data[*].topicTags").value(
                        org.hamcrest.Matchers.everyItem(containsString("story"))));
    }

    @Test
    void newsDetailLookupAndTranslateRemainAvailable() throws Exception {
        String username = "newsreader" + System.nanoTime();
        registerUser(username, "News Reader");
        MockHttpSession session = login(username, "userpass123");
        MvcResult recommendations = mockMvc.perform(get("/api/news/recommendations").session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(recommendations.getResponse().getContentAsString());
        long articleId = root.path("data").path(0).path("id").asLong();

        mockMvc.perform(get("/api/news/" + articleId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").isNotEmpty())
                .andExpect(jsonPath("$.data.content").isNotEmpty())
                .andExpect(jsonPath("$.data.readProgress").value(0))
                .andExpect(jsonPath("$.data.finished").value(false));

        mockMvc.perform(get("/api/news/lookup").param("word", "learning").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.word").isNotEmpty())
                .andExpect(jsonPath("$.data.meaningCn").isNotEmpty());

        mockMvc.perform(post("/api/news/translate")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Learning English",
                                  "content": "Reading helps students learn new words."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.titleCn").exists())
                .andExpect(jsonPath("$.data.contentCn").exists());
    }

    @Test
    void articleProgressAndLookupAreScopedToCurrentUser() throws Exception {
        String username = "reader" + System.nanoTime();
        long userId = registerUser(username, "Reader");
        MockHttpSession session = login(username, "userpass123");
        long otherUserId = registerUser("readerOther" + System.nanoTime(), "Other Reader");

        MvcResult recommendations = mockMvc.perform(get("/api/news/recommendations").session(session))
                .andExpect(status().isOk())
                .andReturn();
        long articleId = objectMapper.readTree(recommendations.getResponse().getContentAsString())
                .path("data").path(0).path("id").asLong();

        mockMvc.perform(get("/api/news/" + articleId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readProgress").value(0))
                .andExpect(jsonPath("$.data.finished").value(false));
        mockMvc.perform(get("/api/news/" + articleId + "/lookup").param("word", "learning").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.word").isNotEmpty());
        mockMvc.perform(get("/api/news/" + articleId + "/lookup").param("word", "practice").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/report/summary").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readingCount").value(0));

        MockHttpSession admin = adminSession();
        mockMvc.perform(get("/api/admin/users/" + userId + "/profile").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readingOverview.readings").value(1))
                .andExpect(jsonPath("$.data.readingOverview.finishedReadings").value(0))
                .andExpect(jsonPath("$.data.readingOverview.lookedUpWords").value(2));
        mockMvc.perform(get("/api/admin/users/" + otherUserId + "/profile").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readingOverview.lookedUpWords").value(0));

        mockMvc.perform(get("/api/admin/users/" + userId + "/readings").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].readProgress").value(0))
                .andExpect(jsonPath("$.data[0].finished").value(false));

        mockMvc.perform(post("/api/news/" + articleId + "/progress")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "readProgress": 40
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readProgress").value(40))
                .andExpect(jsonPath("$.data.finished").value(false));

        mockMvc.perform(post("/api/news/" + articleId + "/progress")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "readProgress": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readProgress").value(40))
                .andExpect(jsonPath("$.data.finished").value(false));

        mockMvc.perform(post("/api/news/" + articleId + "/progress")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "readProgress": 90
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readProgress").value(90))
                .andExpect(jsonPath("$.data.finished").value(true));

        mockMvc.perform(get("/api/report/summary").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readingCount").value(1));

        mockMvc.perform(get("/api/admin/users/" + userId + "/readings").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].readProgress").value(90))
                .andExpect(jsonPath("$.data[0].finished").value(true))
                .andExpect(jsonPath("$.data[0].lookedUpWords").value(2));
    }

    @Test
    void recommendationsPreferInProgressArticlesOverFinishedArticles() throws Exception {
        String username = "recommend" + System.nanoTime();
        registerUser(username, "Recommend User");
        MockHttpSession session = login(username, "userpass123");

        MvcResult initial = mockMvc.perform(get("/api/news/recommendations").session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode articles = objectMapper.readTree(initial.getResponse().getContentAsString()).path("data");
        assertTrue(articles.size() > 1);
        long finishedArticleId = articles.path(0).path("id").asLong();
        long inProgressArticleId = articles.path(1).path("id").asLong();

        mockMvc.perform(get("/api/news/" + finishedArticleId).session(session))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/news/" + finishedArticleId + "/progress")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "readProgress": 100,
                                  "finished": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finished").value(true));

        mockMvc.perform(get("/api/news/" + inProgressArticleId).session(session))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/news/" + inProgressArticleId + "/progress")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "readProgress": 40
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finished").value(false));

        mockMvc.perform(get("/api/news/recommendations").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(inProgressArticleId));
    }

    @Test
    void readingLookupCanSaveWordToReviewBookWithoutDuplicates() throws Exception {
        String username = "saveword" + System.nanoTime();
        registerUser(username, "Save Word User");
        MockHttpSession session = login(username, "userpass123");

        MvcResult recommendations = mockMvc.perform(get("/api/news/recommendations").session(session))
                .andExpect(status().isOk())
                .andReturn();
        long articleId = objectMapper.readTree(recommendations.getResponse().getContentAsString())
                .path("data").path(0).path("id").asLong();

        mockMvc.perform(get("/api/news/" + articleId + "/lookup").param("word", "learning").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.savedInReviewBook").value(false));

        mockMvc.perform(post("/api/dictionary/save").param("word", "learning").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.savedInReviewBook").value(true));
        mockMvc.perform(post("/api/dictionary/save").param("word", "learning").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.savedInReviewBook").value(true));

        mockMvc.perform(get("/api/news/" + articleId + "/lookup").param("word", "learning").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.savedInReviewBook").value(true));

        mockMvc.perform(get("/api/report/summary").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewWords").value(1));
    }

    @Test
    void wordAiDetailFallsBackToLocalRulesWhenModelUnavailable() throws Exception {
        String username = "aidetail" + System.nanoTime();
        registerUser(username, "AI Detail User");
        MockHttpSession session = login(username, "userpass123");

        mockMvc.perform(get("/api/words/1/ai-detail").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.modelName").value("local-rules"))
                .andExpect(jsonPath("$.data.examples.length()", greaterThan(0)))
                .andExpect(jsonPath("$.data.usageNotes.length()", greaterThan(0)));
    }

    @Test
    void reportSuggestionsReflectUserProgress() throws Exception {
        String username = "report" + System.nanoTime();
        registerUser(username, "Report User");
        MockHttpSession session = login(username, "userpass123");

        mockMvc.perform(get("/api/report/summary").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weakPoints", hasSize(greaterThan(0))))
                .andExpect(content().string(containsString("阅读输入不足")))
                .andExpect(content().string(containsString("输出练习不足")));

        MvcResult recommendations = mockMvc.perform(get("/api/news/recommendations").session(session))
                .andExpect(status().isOk())
                .andReturn();
        long articleId = objectMapper.readTree(recommendations.getResponse().getContentAsString())
                .path("data").path(0).path("id").asLong();
        mockMvc.perform(get("/api/news/" + articleId + "/lookup").param("word", "learning").session(session))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/news/" + articleId + "/progress")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "readProgress": 90
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/report/summary").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("提交 1 篇 80-120 词短作文批改")))
                .andExpect(content().string(containsString("收藏 5 个高频词")));
    }

    @Test
    void inputBoundariesReturnBadRequest() throws Exception {
        MockHttpSession session = studentSession();
        String longMessageBody = objectMapper.writeValueAsString(Map.of(
                "scene", "daily",
                "message", "x".repeat(2001)
        ));

        mockMvc.perform(post("/api/chat")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(longMessageBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/news/lookup").param("word", " ").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void realtimeNewsEndpointsAreRemoved() throws Exception {
        MockHttpSession session = studentSession();

        mockMvc.perform(get("/api/news/gsearch")
                        .param("q", "technology")
                        .session(session))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/news/fetch-content")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedApiRequestsReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/words/today").param("limit", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void aiSettingsAreStoredPerUser() throws Exception {
        MockHttpSession student = studentSession();
        MockHttpSession admin = adminSession();

        mockMvc.perform(put("/api/ai/settings")
                        .session(student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "baseUrl": "https://student.example.com/v1",
                                  "apiKey": "student-key",
                                  "chatModel": "student-model"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseUrl").value("https://student.example.com/v1"))
                .andExpect(jsonPath("$.data.chatModel").value("student-model"))
                .andExpect(jsonPath("$.data.hasApiKey").value(true));

        mockMvc.perform(put("/api/ai/settings")
                        .session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "baseUrl": "https://admin.example.com/v1",
                                  "apiKey": "admin-key",
                                  "chatModel": "admin-model"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseUrl").value("https://admin.example.com/v1"))
                .andExpect(jsonPath("$.data.chatModel").value("admin-model"))
                .andExpect(jsonPath("$.data.hasApiKey").value(true));

        mockMvc.perform(get("/api/ai/settings").session(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseUrl").value("https://student.example.com/v1"))
                .andExpect(jsonPath("$.data.chatModel").value("student-model"));

        mockMvc.perform(get("/api/ai/settings").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseUrl").value("https://admin.example.com/v1"))
                .andExpect(jsonPath("$.data.chatModel").value("admin-model"));
    }

    @Test
    void adminApisRequireAdminAndExposeUserProfile() throws Exception {
        mockMvc.perform(get("/api/admin/summary").session(studentSession()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/admin/summary").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.users").isNumber())
                .andExpect(jsonPath("$.data.activeUsers").isNumber())
                .andExpect(jsonPath("$.data.todayWordReviews").isNumber())
                .andExpect(jsonPath("$.data.todayUniqueWords").isNumber())
                .andExpect(jsonPath("$.data.todayLearners").isNumber())
                .andExpect(jsonPath("$.data.dailyWordStats.length()").value(7));

        mockMvc.perform(get("/api/admin/users/1/profile").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").isNotEmpty())
                .andExpect(jsonPath("$.data.level").isNumber())
                .andExpect(jsonPath("$.data.favoriteWords").isNumber())
                .andExpect(jsonPath("$.data.learnedWords").isNumber())
                .andExpect(jsonPath("$.data.reviewLogs").isNumber())
                .andExpect(jsonPath("$.data.finishedReadings").isNumber())
                .andExpect(jsonPath("$.data.vocabularyDistribution.learnedWords").isNumber())
                .andExpect(jsonPath("$.data.readingOverview.readings").isNumber())
                .andExpect(jsonPath("$.data.aiConfigured").isBoolean());
    }

    @Test
    void registeredAccountsStayUserAndCanBeManagedByAdmin() throws Exception {
        String username = "managed" + System.nanoTime();
        long userId = registerUser(username, "Before Edit");
        MockHttpSession admin = adminSession();

        mockMvc.perform(put("/api/admin/users/" + userId)
                        .session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "After Edit",
                                  "status": "ACTIVE",
                                  "password": "newpass123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.nickname").value("After Edit"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "newpass123"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));

        mockMvc.perform(post("/api/admin/users/" + userId + "/status")
                        .session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void adminAccountCannotBeDisabled() throws Exception {
        long adminUserId = currentUserId(adminSession());

        mockMvc.perform(post("/api/admin/users/" + adminUserId + "/status")
                        .session(adminSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void adminCanCreateUpdateSearchAndDeleteReadingArticles() throws Exception {
        MockHttpSession admin = adminSession();
        String title = "Admin Reading " + System.nanoTime();
        MvcResult created = mockMvc.perform(post("/api/admin/news")
                        .session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "summary": "Short summary",
                                  "content": "This article is maintained from the admin console.",
                                  "sourceName": "Admin Test",
                                  "difficulty": "easy",
                                  "topicTags": "admin,test"
                                }
                                """.formatted(title)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(title))
                .andReturn();
        long articleId = objectMapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(put("/api/admin/news/" + articleId)
                        .session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s Updated",
                                  "summary": "Updated summary",
                                  "content": "Updated content",
                                  "sourceName": "Admin Test",
                                  "difficulty": "medium",
                                  "topicTags": "admin,updated"
                                }
                                """.formatted(title)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(title + " Updated"))
                .andExpect(jsonPath("$.data.difficulty").value("medium"));

        mockMvc.perform(get("/api/admin/news")
                        .session(admin)
                        .param("search", title)
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total", greaterThan(0)))
                .andExpect(content().string(containsString(title + " Updated")));

        mockMvc.perform(delete("/api/admin/news/" + articleId).session(admin))
                .andExpect(status().isOk());
    }

    private ReviewWord seedWrongReviewWord() throws Exception {
        String username = "review" + System.nanoTime();
        registerUser(username, "Review User");
        MockHttpSession session = login(username, "userpass123");
        MvcResult words = mockMvc.perform(get("/api/words/today")
                        .param("limit", "1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andReturn();
        JsonNode wordNode = objectMapper.readTree(words.getResponse().getContentAsString()).path("data").path(0);
        long wordId = wordNode.path("id").asLong();
        String word = wordNode.path("word").asText();
        String meaningCn = wordNode.path("meaningCn").asText();

        mockMvc.perform(post("/api/words/" + wordId + "/quiz")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answer": "definitely-not-the-answer"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(false));

        return new ReviewWord(session, wordId, word, meaningCn);
    }

    private String reviewBody(String spellingAnswer, String meaningAnswer) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "spellingAnswer", spellingAnswer,
                "meaningAnswer", meaningAnswer
        ));
    }

    private boolean responseContainsWord(MvcResult result, long wordId) throws Exception {
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        for (JsonNode item : data) {
            if (item.path("id").asLong() == wordId) {
                return true;
            }
        }
        return false;
    }

    private record ReviewWord(MockHttpSession session, long wordId, String word, String meaningCn) {
    }

    private MockHttpSession studentSession() throws Exception {
        return login("student", "student123");
    }

    private MockHttpSession adminSession() throws Exception {
        return login("admin", "admin123");
    }

    private long registerUser(String username, String nickname) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "nickname": "%s",
                                  "password": "userpass123"
                                }
                                """.formatted(username, nickname)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }

    private long currentUserId(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }

    private MockHttpSession login(String username, String password) throws Exception {
        String body = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
