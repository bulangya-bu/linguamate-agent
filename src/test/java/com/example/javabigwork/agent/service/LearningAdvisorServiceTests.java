package com.example.javabigwork.agent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.javabigwork.agent.dto.LearningAdvisorView;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.auth.entity.UserAccount;
import com.example.javabigwork.auth.mapper.UserAccountMapper;
import com.example.javabigwork.chat.entity.Conversation;
import com.example.javabigwork.chat.entity.Message;
import com.example.javabigwork.chat.mapper.ConversationMapper;
import com.example.javabigwork.chat.mapper.MessageMapper;
import com.example.javabigwork.news.entity.ReadingRecord;
import com.example.javabigwork.news.mapper.ReadingRecordMapper;
import com.example.javabigwork.vocabulary.entity.WordReviewLog;
import com.example.javabigwork.vocabulary.entity.UserWordProgress;
import com.example.javabigwork.vocabulary.entity.Word;
import com.example.javabigwork.vocabulary.mapper.UserWordProgressMapper;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import com.example.javabigwork.vocabulary.mapper.WordReviewLogMapper;
import com.example.javabigwork.writing.entity.EssaySubmission;
import com.example.javabigwork.writing.mapper.EssaySubmissionMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "linguamate.cache.redis.enabled=false")
@Transactional
class LearningAdvisorServiceTests {

    @Autowired private LearningAdvisorService service;
    @Autowired private UserContext userContext;
    @Autowired private UserAccountMapper userAccountMapper;
    @Autowired private ConversationMapper conversationMapper;
    @Autowired private MessageMapper messageMapper;
    @Autowired private WordReviewLogMapper wordReviewLogMapper;
    @Autowired private UserWordProgressMapper userWordProgressMapper;
    @Autowired private WordMapper wordMapper;
    @Autowired private EssaySubmissionMapper essaySubmissionMapper;
    @Autowired private ReadingRecordMapper readingRecordMapper;

    private UserAccount user;

    @BeforeEach
    void setUp() {
        user = new UserAccount();
        user.username = "advisor-" + UUID.randomUUID();
        user.passwordHash = "test";
        user.nickname = "学习者";
        user.role = "STUDENT";
        user.status = "ACTIVE";
        user.createdAt = LocalDateTime.now();
        user.updatedAt = user.createdAt;
        userAccountMapper.insert(user);
        userContext.set(user);
    }

    @AfterEach
    void tearDown() {
        userContext.clear();
    }

    @Test
    void dailyRouteRecommendsOnlyIncompleteLearningSteps() {
        LearningAdvisorView view = service.dailyRoute(false);

        assertEquals("daily-route", view.learningCard().type());
        assertEquals(5, view.learningCard().items().size());
        assertEquals("complete", view.learningCard().items().get(2).status());
        assertEquals(4, view.actions().size());
        assertEquals("chat", view.actions().get(0).section());
        assertEquals("vocabulary", view.actions().get(1).section());
        assertEquals("writing", view.actions().get(2).section());
        assertEquals("news", view.actions().get(3).section());
    }

    @Test
    void dailyRouteMarksEveryStepCompleteFromTodaysPersistedActivity() {
        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = new Conversation();
        conversation.userId = user.id;
        conversation.scene = "daily";
        conversation.title = "today";
        conversation.createdAt = now;
        conversation.updatedAt = now;
        conversationMapper.insert(conversation);
        insertMessage(conversation.id, "user", now);
        insertMessage(conversation.id, "assistant", now);

        for (int index = 0; index < 6; index++) {
            WordReviewLog log = new WordReviewLog();
            log.userId = user.id;
            log.wordId = (long) index + 1;
            log.reviewType = "QUIZ";
            log.correct = true;
            log.createdAt = now;
            wordReviewLogMapper.insert(log);
        }

        EssaySubmission essay = new EssaySubmission();
        essay.userId = user.id;
        essay.title = "today";
        essay.originalText = "A short test essay.";
        essay.createdAt = now;
        essaySubmissionMapper.insert(essay);

        ReadingRecord reading = new ReadingRecord();
        reading.userId = user.id;
        reading.articleId = 1L;
        reading.finished = true;
        reading.readProgress = 100;
        reading.createdAt = now;
        reading.updatedAt = now;
        readingRecordMapper.insert(reading);

        LearningAdvisorView view = service.dailyRoute(false);

        assertTrue(view.learningCard().items().stream()
                .allMatch(item -> "complete".equals(item.status())));
        assertTrue(view.actions().isEmpty());
    }

    @Test
    void vocabularySummarySeparatesMasteredAndUnfamiliarWords() {
        seedProgress("steady", 3, 0);
        seedProgress("fragile", 4, 1);
        seedProgress("newish", 2, 0);

        LearningAdvisorView mastered = service.vocabularySummary("mastered", 0, 10);
        LearningAdvisorView unfamiliar = service.vocabularySummary("unfamiliar", 0, 10);

        assertEquals(1, mastered.learningCard().totalElements());
        assertEquals("/wordbank", mastered.actions().get(0).route());
        assertEquals("mastered", mastered.actions().get(0).query().get("scope"));
        assertTrue(mastered.learningCard().items().get(0).label().startsWith("steady-"));
        assertEquals(2, unfamiliar.learningCard().totalElements());
        assertTrue(unfamiliar.learningCard().items().stream()
                .map(item -> item.label())
                .toList()
                .stream()
                .anyMatch(label -> label.startsWith("fragile-")));
        assertTrue(unfamiliar.learningCard().items().stream()
                .map(item -> item.label())
                .anyMatch(label -> label.startsWith("newish-")));
    }

    @Test
    void vocabularySummaryClampsSizeAndMovesOverflowToLastPage() {
        for (int index = 0; index < 23; index++) {
            seedProgress("weak-" + index, 1, 0);
        }

        LearningAdvisorView view = service.vocabularySummary("unfamiliar", 99, 50);

        assertEquals(20, view.learningCard().size());
        assertEquals(1, view.learningCard().page());
        assertEquals(2, view.learningCard().totalPages());
        assertEquals(3, view.learningCard().items().size());
    }

    @Test
    void unfamiliarWordsWithoutDueOrMistakeRecordsLeadToVocabularyPractice() {
        seedProgress("newly-learned", 1, 0);
        LearningAdvisorView view = service.vocabularySummary("unfamiliar", 0, 10);
        assertEquals("/vocabulary", view.actions().get(0).route());
        assertTrue(view.actions().get(0).query().isEmpty());
    }

    @Test
    void snapshotIsBoundedAndDistinguishesMasteredDueAndMistakeWords() {
        seedProgress("mastered", 4, 0);
        for (int index = 0; index < 14; index++) seedProgress("weak-" + index, 1, index + 1);
        var snapshot = service.assistantSnapshot();
        assertEquals(15, snapshot.trackedWords());
        assertEquals(1, snapshot.masteredWords());
        assertEquals(14, snapshot.unfamiliarWords());
        assertEquals(14, snapshot.mistakeWords());
        assertEquals(0, snapshot.dueWords());
        assertEquals(12, snapshot.priorityWords().size());
        assertEquals(14, snapshot.priorityWords().get(0).wrongCount());
        assertTrue(snapshot.priorityWords().stream().noneMatch(word -> word.due()));
    }

    private void insertMessage(Long conversationId, String role, LocalDateTime createdAt) {
        Message message = new Message();
        message.conversationId = conversationId;
        message.role = role;
        message.content = role;
        message.createdAt = createdAt;
        messageMapper.insert(message);
    }

    private void seedProgress(String text, int familiarity, int wrongCount) {
        Word word = new Word();
        word.word = text + "-" + UUID.randomUUID();
        word.meaningCn = "测试释义";
        word.levelTag = "test";
        wordMapper.insert(word);

        UserWordProgress progress = new UserWordProgress();
        progress.userId = user.id;
        progress.wordId = word.id;
        progress.familiarity = familiarity;
        progress.wrongCount = wrongCount;
        progress.reviewCount = 1;
        progress.favorite = false;
        progress.lastReviewAt = LocalDateTime.now();
        userWordProgressMapper.insert(progress);

    }
}
