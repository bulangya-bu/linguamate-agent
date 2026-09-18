# Conversational Learning Advisor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let LinguaMate answer current-user questions about unfinished daily learning and vocabulary mastery with deterministic, paginated chat cards and safe module-navigation actions.

**Architecture:** A new user-scoped `LearningAdvisorService` calculates daily-route and vocabulary-profile snapshots from existing tables. `AgentOrchestrator` recognizes advisor intents before online-model fallback and returns typed card/action DTOs through the existing chat API; a small read-only endpoint serves vocabulary pagination. Vue renders the optional structures inside assistant messages and routes actions through a fixed handler.

**Tech Stack:** Java 17, Spring Boot 3.5.3, MyBatis-Plus, MockMvc/JUnit 5, Vue 3 Composition API, Vue Router, vanilla CSS, Vite.

**Spec:** `docs/superpowers/specs/2026-09-18-conversational-learning-advisor-design.md`

## Global Constraints

- Preserve the Agent trace stages `Observe`, `Think`, `Act`, and `Answer`.
- All queries must derive the user ID from `UserContext`; clients never supply a user ID.
- Daily completion uses the server-local half-open interval `[todayStart, tomorrowStart)`.
- Daily thresholds are: one completed exchange, six `QUIZ` submissions, empty existing wrong-word queue, one essay submission, and one finished reading.
- Mastered means `familiarity >= 3` and no outstanding wrong count; unfamiliar means `wrong_count > 0` or `familiarity < 3` for a recorded word.
- Vocabulary pagination accepts sizes 1–20 and the chat UI requests 10.
- Model/network failures must not prevent advisor answers.
- Navigation routes come only from a server-side allowlist.
- Preserve unrelated user changes already present in the working tree.

---

### Task 1: Define the structured advisor contract and daily-route calculation

**Files:**
- Create: `src/main/java/com/example/javabigwork/agent/dto/ChatAction.java`
- Create: `src/main/java/com/example/javabigwork/agent/dto/LearningCardItem.java`
- Create: `src/main/java/com/example/javabigwork/agent/dto/LearningCard.java`
- Create: `src/main/java/com/example/javabigwork/agent/dto/LearningAdvisorView.java`
- Create: `src/main/java/com/example/javabigwork/agent/service/LearningAdvisorService.java`
- Modify: `src/test/java/com/example/javabigwork/JavaBigWorkApplicationTests.java`

**Interfaces:**
- Consumes: `UserContext.userId()`, `ConversationMapper`, `MessageMapper`, `WordReviewLogMapper`, `UserWordProgressMapper`, `EssaySubmissionMapper`, and `ReadingRecordMapper`.
- Produces: `LearningAdvisorService.dailyRoute(boolean includeCurrentExchange): LearningAdvisorView`.
- Produces: immutable records `ChatAction(id, label, section, route, Map<String,String> query)`, `LearningCardItem(key, label, detail, status)`, `LearningCard(type, title, summary, group, items, page, size, totalElements, totalPages)`, and `LearningAdvisorView(reply, learningCard, actions)`.

- [ ] **Step 1: Write failing integration tests for an empty and in-flight daily route**

Add tests using a newly registered student session. Assert the real service/API-visible contract, not mapper calls:

```java
@Autowired
private LearningAdvisorService learningAdvisorService;

@Test
void dailyRouteListsEveryUnfinishedModuleForNewUser() {
    long userId = registerUser("route" + System.nanoTime(), "Route User");
    userContext.set(userAccountMapper.selectById(userId));
    try {
        LearningAdvisorView view = learningAdvisorService.dailyRoute(false);
        assertEquals("DAILY_ROUTE", view.learningCard().type());
        assertEquals(5, view.learningCard().items().size());
        assertEquals(List.of("chat", "vocabulary", "writing", "news"),
                view.actions().stream().map(ChatAction::section).toList());
        assertEquals("complete", view.learningCard().items().get(2).status());
    } finally {
        userContext.clear();
    }
}

@Test
void dailyRouteCountsTheExchangeCurrentlyBeingAnswered() {
    long userId = registerUser("exchange" + System.nanoTime(), "Exchange User");
    userContext.set(userAccountMapper.selectById(userId));
    try {
        LearningAdvisorView view = learningAdvisorService.dailyRoute(true);
        assertEquals("complete", view.learningCard().items().get(0).status());
        assertFalse(view.actions().stream().anyMatch(action -> action.section().equals("chat")));
    } finally {
        userContext.clear();
    }
}
```

Autowire `UserContext` and `UserAccountMapper` in the existing test class. Keep the `try/finally` cleanup shown above so one test's current user cannot leak into another.

- [ ] **Step 2: Run the two tests and verify RED**

Run:

```powershell
.\mvnw.cmd -Dtest=JavaBigWorkApplicationTests#dailyRouteListsEveryUnfinishedModuleForNewUser+dailyRouteCountsTheExchangeCurrentlyBeingAnswered test
```

Expected: compilation fails because the advisor DTOs/service do not exist.

- [ ] **Step 3: Add the immutable DTO records**

Implement exactly these shapes:

```java
public record ChatAction(String id, String label, String section, String route, Map<String, String> query) {}

public record LearningCardItem(String key, String label, String detail, String status) {}

public record LearningCard(
        String type,
        String title,
        String summary,
        String group,
        List<LearningCardItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}

public record LearningAdvisorView(String reply, LearningCard learningCard, List<ChatAction> actions) {}
```

Use `List.copyOf`/`Map.copyOf` in compact constructors so callers cannot mutate response data.

- [ ] **Step 4: Implement the minimal daily-route service**

Create a Spring `@Service` that calculates `todayStart` and `tomorrowStart`, queries only the current user's records, and builds five ordered items. Resolve user-owned conversation IDs before querying messages. For persisted conversation completion require at least one user message and one assistant message today; set chat complete immediately when `includeCurrentExchange` is true.

Use fixed actions:

```java
private static final Map<String, RouteAction> ROUTES = Map.of(
    "chat", new RouteAction("继续对话", "chat", "/chat", Map.of()),
    "vocabulary", new RouteAction("去背词", "vocabulary", "/vocabulary", Map.of()),
    "review", new RouteAction("去复习", "review", "/review", Map.of("mode", "today")),
    "writing", new RouteAction("去写作", "writing", "/writing", Map.of()),
    "news", new RouteAction("去阅读", "news", "/news", Map.of())
);
```

Count vocabulary with `review_type = "QUIZ"`; review is complete when no `user_word_progress` row has `wrong_count > 0`; writing checks `essay_submission.created_at`; reading checks `finished = true` and `updated_at`. Use `ge` and `lt` for the day boundaries.

- [ ] **Step 5: Run the focused tests and verify GREEN**

Run the command from Step 2. Expected: both tests pass.

- [ ] **Step 6: Add and pass partial/all-complete and user-isolation tests**

Seed six quiz logs, one essay, one finished reading, and no wrong-word row for user A; seed a wrong word for user B. Assert A reports all five complete with no actions while B's data never appears in A's card. Add a record at `todayStart.minusNanos(1)` and assert it does not count, plus one at `todayStart` and assert it does.

Run:

```powershell
.\mvnw.cmd -Dtest=JavaBigWorkApplicationTests#dailyRoute* test
```

Expected: all daily-route tests pass.

- [ ] **Step 7: Commit the daily-route slice**

```powershell
git add src/main/java/com/example/javabigwork/agent/dto src/main/java/com/example/javabigwork/agent/service/LearningAdvisorService.java src/test/java/com/example/javabigwork/JavaBigWorkApplicationTests.java
git commit -m "feat: calculate personalized daily learning route"
```

### Task 2: Add vocabulary classification and paginated read API

**Files:**
- Modify: `src/main/java/com/example/javabigwork/agent/service/LearningAdvisorService.java`
- Create: `src/main/java/com/example/javabigwork/agent/controller/LearningAdvisorController.java`
- Modify: `src/test/java/com/example/javabigwork/JavaBigWorkApplicationTests.java`

**Interfaces:**
- Consumes: the DTO records created in Task 1, `UserWordProgressMapper`, and `WordMapper`.
- Produces: `LearningAdvisorService.vocabularyProfile(String group, int page, int size): LearningAdvisorView`.
- Produces: `GET /api/learning-advisor/vocabulary?group={unfamiliar|mastered}&page={n}&size={n}` returning `ApiResponse<LearningAdvisorView>`.

- [ ] **Step 1: Write failing classification and pagination tests**

Seed these literal cases for the active user:

```text
alpha: familiarity 4, wrongCount 0 -> MASTERED
beta:  familiarity 4, wrongCount 1 -> UNFAMILIAR
gamma: familiarity 2, wrongCount 0 -> UNFAMILIAR
delta: familiarity 3, wrongCount 0 -> MASTERED
```

Test `vocabularyProfile("mastered", 0, 10)` returns alpha/delta only and `vocabularyProfile("unfamiliar", 0, 10)` returns beta/gamma only. Seed 12 additional unfamiliar words and assert pages 0 and 1 contain 10 and 4 items with `totalElements = 14` and `totalPages = 2`. Assert user B's words do not appear.

- [ ] **Step 2: Run the vocabulary tests and verify RED**

```powershell
.\mvnw.cmd -Dtest=JavaBigWorkApplicationTests#vocabularyAdvisor* test
```

Expected: compilation fails because `vocabularyProfile` does not exist.

- [ ] **Step 3: Implement classification, stable ordering, and page clamping**

Normalize group to lowercase and accept only `unfamiliar` and `mastered`. Clamp size with `Math.max(1, Math.min(20, size))`; reject negative pages. Query current-user progress rows, load referenced words, discard missing words, apply the exact precedence rules, and sort by:

- unfamiliar: wrong count descending, familiarity ascending, last review ascending;
- mastered: familiarity descending, last review descending.

Clamp an overlarge page to the last valid page. Build details such as `熟悉度 2/5 · 错误 1 次`, use card group values `UNFAMILIAR`/`MASTERED`, and return the fixed Review action with `mode=today` or `mode=mastered`.

- [ ] **Step 4: Run the service tests and verify GREEN**

Run the command from Step 2. Expected: all vocabulary advisor tests pass.

- [ ] **Step 5: Write failing MockMvc tests for the endpoint boundary**

```java
mockMvc.perform(get("/api/learning-advisor/vocabulary")
        .session(studentSession)
        .param("group", "unfamiliar")
        .param("page", "0")
        .param("size", "10"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.learningCard.type").value("VOCABULARY_PROFILE"))
    .andExpect(jsonPath("$.data.learningCard.items").isArray())
    .andExpect(jsonPath("$.data.actions[0].route").value("/review"));
```

Also assert invalid `group=unknown` and `page=-1` use the existing bad-request error shape, and unauthenticated access is rejected by existing auth handling.

- [ ] **Step 6: Run endpoint tests and verify RED**

Expected: 404 because the controller route is absent.

- [ ] **Step 7: Implement the thin controller and verify GREEN**

The controller delegates directly to the service and returns `ApiResponse.ok(...)`; it does not accept a user ID.

Run all `vocabularyAdvisor*` tests. Expected: pass.

- [ ] **Step 8: Commit the vocabulary slice**

```powershell
git add src/main/java/com/example/javabigwork/agent/service/LearningAdvisorService.java src/main/java/com/example/javabigwork/agent/controller/LearningAdvisorController.java src/test/java/com/example/javabigwork/JavaBigWorkApplicationTests.java
git commit -m "feat: expose paginated vocabulary insights"
```

### Task 3: Integrate advisor intents into the Agent and chat response

**Files:**
- Modify: `src/main/java/com/example/javabigwork/agent/AgentResult.java`
- Modify: `src/main/java/com/example/javabigwork/agent/AgentOrchestrator.java`
- Modify: `src/main/java/com/example/javabigwork/chat/dto/ChatResponse.java`
- Modify: `src/main/java/com/example/javabigwork/chat/service/ChatService.java`
- Modify: `src/test/java/com/example/javabigwork/JavaBigWorkApplicationTests.java`

**Interfaces:**
- Consumes: `LearningAdvisorService.dailyRoute(true)` and `vocabularyProfile(group, 0, 10)`.
- Produces: `AgentResult(reply, trace, suggestions, LearningCard learningCard, List<ChatAction> actions)`.
- Produces: `ChatResponse(conversationId, reply, trace, suggestions, learningCard, actions)` while preserving the existing JSON fields.

- [ ] **Step 1: Write failing chat intent tests**

Use MockMvc with the unavailable AI configuration already defined by the test class:

```java
mockMvc.perform(post("/api/chat")
        .session(studentSession)
        .contentType(APPLICATION_JSON)
        .content("""{"scene":"daily","message":"今天还有什么没学？"}"""))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.learningCard.type").value("DAILY_ROUTE"))
    .andExpect(jsonPath("$.data.actions[0].section").exists())
    .andExpect(jsonPath("$.data.trace[1].phase").value("Think"));
```

Add analogous tests for “总结我不熟悉的单词”, “查看已掌握的单词”, and English phrases “what should I learn today” / “show my mastered words”. Assert an ordinary grammar question has no `learningCard` and still reaches the current RAG/fallback branch.

- [ ] **Step 2: Run the intent tests and verify RED**

```powershell
.\mvnw.cmd -Dtest=JavaBigWorkApplicationTests#chatAdvisor* test
```

Expected: missing `learningCard` assertions fail.

- [ ] **Step 3: Extend response records without breaking old callers**

Add optional card/actions fields to `AgentResult` and `ChatResponse`. Add convenience factories or overloaded constructors that default to `null` and `List.of()` so existing WORD/NEWS/ESSAY/CHAT branches remain concise and source-compatible.

- [ ] **Step 4: Implement deterministic advisor intent matching**

Add intents `DAILY_PROGRESS`, `VOCABULARY_UNFAMILIAR`, and `VOCABULARY_MASTERED` before the broad WORD branch. Match phrase groups rather than single ambiguous words:

```text
DAILY_PROGRESS: 今天+没学/未完成/学什么/下一步, 今日+进度/路线, learn+today, unfinished+today
VOCABULARY_UNFAMILIAR: 不熟悉/薄弱/易错/错词 + 单词/词汇, unfamiliar/weak/wrong + word/vocabulary
VOCABULARY_MASTERED: 已掌握/会的 + 单词/词汇, mastered/known + word/vocabulary
```

For advisor intents, call the service, add an `Act` trace naming `LearningAdvisorService`, and return its reply/card/actions without invoking `WebAnswerService`. Preserve `Answer` as the final trace step.

Wrap only the advisor service call in `try/catch`. On calculation failure, log the exception, add an `Act` trace explaining that learning data could not be loaded, and return the local reply `暂时无法读取你的学习进度，请稍后再试。` with `learningCard = null` and an empty action list. Never ask the model to guess these values.

- [ ] **Step 5: Pass structured fields through ChatService**

Construct `ChatResponse` with `result.learningCard()` and `result.actions()`. Do not serialize action data into the assistant's content or accept a route from the model.

- [ ] **Step 6: Run focused and existing chat tests**

```powershell
.\mvnw.cmd -Dtest=JavaBigWorkApplicationTests#chatAdvisor*+chatTrace* test
```

Expected: advisor and pre-existing chat trace tests pass.

- [ ] **Step 7: Commit the Agent slice**

```powershell
git add src/main/java/com/example/javabigwork/agent/AgentResult.java src/main/java/com/example/javabigwork/agent/AgentOrchestrator.java src/main/java/com/example/javabigwork/chat/dto/ChatResponse.java src/main/java/com/example/javabigwork/chat/service/ChatService.java src/test/java/com/example/javabigwork/JavaBigWorkApplicationTests.java
git commit -m "feat: answer personalized learning questions in chat"
```

### Task 4: Render interactive learning cards in Vue

**Files:**
- Create: `frontend/src/learning-advisor.js`
- Create: `frontend/src/learning-advisor.test.js`
- Modify: `frontend/src/api/modules.js`
- Modify: `frontend/src/views/MainAppView.vue`
- Modify: `frontend/src/styles/app.css`
- Modify: `frontend/package.json`

**Interfaces:**
- Consumes: chat `learningCard`/`actions` and `learningApi.vocabularyAdvisor({group,page,size})`.
- Produces: `advisorMessage(result)`, `actionLocation(action)`, and `replaceAdvisorCard(message, view)` pure helpers.
- Produces: `runChatAction(action)` and `changeAdvisorPage(message, page)` component handlers.

- [ ] **Step 1: Write failing pure frontend tests**

Use Node's built-in test runner, adding no test dependency:

```js
import test from "node:test";
import assert from "node:assert/strict";
import { actionLocation, advisorMessage, replaceAdvisorCard } from "./learning-advisor.js";

test("advisorMessage keeps structured chat response fields", () => {
  const message = advisorMessage({ reply: "还有两项", learningCard: { type: "DAILY_ROUTE" }, actions: [{ route: "/review", query: { mode: "today" } }] });
  assert.equal(message.role, "assistant");
  assert.equal(message.learningCard.type, "DAILY_ROUTE");
  assert.equal(message.actions.length, 1);
});

test("actionLocation accepts only known application routes", () => {
  assert.deepEqual(actionLocation({ route: "/review", query: { mode: "mastered" } }), { path: "/review", query: { mode: "mastered" } });
  assert.deepEqual(actionLocation({ route: "https://example.com", query: {} }), { path: "/" });
});

test("replaceAdvisorCard updates one message without appending a duplicate", () => {
  const original = { role: "assistant", content: "词汇总结", learningCard: { page: 0 }, actions: [] };
  const updated = replaceAdvisorCard(original, { learningCard: { page: 1 }, actions: [{ route: "/review" }] });
  assert.equal(updated.content, "词汇总结");
  assert.equal(updated.learningCard.page, 1);
});
```

Add `"test": "node --test src/*.test.js"` to `frontend/package.json`.

- [ ] **Step 2: Run frontend tests and verify RED**

```powershell
npm --prefix frontend test
```

Expected: module-not-found failure for `learning-advisor.js`.

- [ ] **Step 3: Implement the pure helpers and verify GREEN**

Allow only `/`, `/chat`, `/vocabulary`, `/review`, `/writing`, `/news`, and `/wordbank`; sanitize query to plain string values. Preserve message content and replace only card/actions during pagination.

Run `npm --prefix frontend test`. Expected: 3 tests pass.

- [ ] **Step 4: Add the advisor pagination API method**

Add:

```js
vocabularyAdvisor: (params) => api(`/api/learning-advisor/vocabulary${query(params)}`)
```

to `learningApi`.

- [ ] **Step 5: Attach structures to new assistant messages and render the card**

Change `sendChat` to push `advisorMessage(result)`. Inside each assistant `<article>`, render when `message.learningCard` exists:

```vue
<div v-if="message.learningCard" class="chat-learning-card" :data-type="message.learningCard.type">
  <div class="chat-learning-card__heading">
    <strong>{{ message.learningCard.title }}</strong>
    <span>{{ message.learningCard.summary }}</span>
  </div>
  <ul class="chat-learning-list">
    <li v-for="item in message.learningCard.items" :key="item.key" :class="`is-${item.status}`">
      <span>{{ item.label }}</span><small>{{ item.detail }}</small>
    </li>
  </ul>
  <div v-if="message.learningCard.totalPages > 1" class="chat-learning-pagination">
    <button type="button" :disabled="message.learningCard.page === 0" @click="changeAdvisorPage(message, message.learningCard.page - 1)">上一页</button>
    <span>{{ message.learningCard.page + 1 }} / {{ message.learningCard.totalPages }}</span>
    <button type="button" :disabled="message.learningCard.page + 1 >= message.learningCard.totalPages" @click="changeAdvisorPage(message, message.learningCard.page + 1)">下一页</button>
  </div>
  <div class="chat-learning-actions">
    <button v-for="action in message.actions" :key="action.id" type="button" @click="runChatAction(action)">{{ action.label }}</button>
  </div>
</div>
```

`changeAdvisorPage` fetches the selected group with size 10 and mutates only that message's card/actions. `runChatAction` passes `actionLocation(action)` to `router.push`.

- [ ] **Step 6: Apply route query state and compact responsive styling**

When entering `/review?mode=mastered|today|favorites`, synchronize `state.reviewMode` and call `loadReviews()`. Style the embedded list with the existing chat surface, one green action treatment, visible focus outlines, wrapping details, 44px mobile tap targets, and `prefers-reduced-motion` disabling the card entrance.

- [ ] **Step 7: Run frontend tests and production build**

```powershell
npm --prefix frontend test
npm --prefix frontend run build
```

Expected: tests pass and Vite exits 0, producing updated assets in `src/main/resources/static` according to the existing Vite output configuration.

- [ ] **Step 8: Commit the frontend slice**

```powershell
git add frontend/src/learning-advisor.js frontend/src/learning-advisor.test.js frontend/src/api/modules.js frontend/src/views/MainAppView.vue frontend/src/styles/app.css frontend/package.json frontend/package-lock.json src/main/resources/static/index.html src/main/resources/static/assets
git commit -m "feat: render learning recommendations in chat"
```

### Task 5: Full regression and interaction verification

**Files:**
- Modify only if verification exposes a defect: files already listed in Tasks 1–4.

**Interfaces:**
- Consumes: the complete advisor backend and frontend.
- Produces: verified end-to-end behavior without new public interfaces.

- [ ] **Step 1: Run the full backend suite**

```powershell
.\mvnw.cmd test
```

Expected: Maven exits 0 with zero test failures and errors.

- [ ] **Step 2: Run frontend tests and build from a clean invocation**

```powershell
npm --prefix frontend test
npm --prefix frontend run build
```

Expected: Node tests and Vite build both exit 0 without errors.

- [ ] **Step 3: Run the application and verify the user flow**

```powershell
.\mvnw.cmd spring-boot:run
```

At `http://localhost:4040/chat`, verify:

1. “今天还有什么没学？” returns a daily card, marks the current chat complete, and routes each button correctly.
2. “总结我不熟悉的单词” returns counts and at most ten words; Next replaces the page in place.
3. “查看已掌握的单词” opens `/review?mode=mastered`, with the mastered tab loaded.
4. A normal English practice prompt produces no advisor card and still returns the normal answer/fallback.
5. At mobile width, labels wrap without overlap and controls remain keyboard accessible.

- [ ] **Step 4: Inspect the scoped diff and confirm no unrelated files were staged**

```powershell
git status --short
git diff --check
git diff --stat HEAD~4..HEAD
```

Expected: no whitespace errors; only advisor, chat, frontend, generated frontend assets, tests, spec, and plan changes belong to this work. Pre-existing unrelated modifications remain unstaged/uncommitted.

- [ ] **Step 5: Commit verification-only fixes if any were required**

Stage each corrected advisor file by its exact path from Tasks 1–4, inspect `git diff --cached --stat`, then run `git commit -m "fix: complete learning advisor verification"`. If no fix was required, do not create an empty commit.
