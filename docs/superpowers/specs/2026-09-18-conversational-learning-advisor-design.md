# Conversational Learning Advisor Design

## Goal

Extend the existing LinguaMate chat so it can answer personalized learning-status questions from the signed-in user's real data. The first supported question families are:

- Daily route progress, such as “今天还有什么没学” and “下一步学什么”.
- Vocabulary profile, such as “我有哪些不熟悉的单词”, “总结已掌握单词”, and “归纳我的词库”.

The reply must remain useful when the online model is unavailable. Structured buttons inside assistant messages let the user open the corresponding learning module. Vocabulary results support server-backed pagination rather than placing the full vocabulary list in one message.

## Product Behaviour

### Daily route

The daily route contains five ordered activities. An activity is complete only when the current user meets its condition during the current local calendar day:

1. Conversation: at least one user/assistant exchange is completed.
2. Vocabulary: at least six vocabulary quiz submissions are completed.
3. Review: the existing today-review queue is empty.
4. Writing: at least one essay is submitted and reviewed.
5. Reading: at least one article is marked finished.

The advisor returns the completed count, the unfinished activities in route order, and a primary recommendation for the first unfinished activity. Each unfinished activity carries a button that navigates to its existing frontend route. If every activity is complete, the response congratulates the user and does not invent another required task.

### Vocabulary profile

Vocabulary uses the existing `user_word_progress` model and current product thresholds:

- Mastered: `familiarity >= 3`.
- Unfamiliar: `wrong_count > 0` or `familiarity < 3` for words with a user progress record.

When a word satisfies both expressions, mastered takes precedence only if its current familiarity is at least 3 and it has no outstanding wrong count. Otherwise it remains unfamiliar. This avoids describing an unresolved wrong word as mastered.

The initial chat card shows:

- The total number of matching words.
- A concise summary using counts and current review urgency.
- At most ten word items, including the word, Chinese meaning, familiarity, and wrong count when relevant.
- Previous and next page controls when applicable.
- A contextual module action: unfamiliar words open Review; mastered words open the mastered tab in Review.

Pagination is zero-based in the API and displayed as one-based in the UI. The server clamps page size to 1–20; the chat UI requests 10. Empty results receive a helpful empty state and a route button to start vocabulary practice.

### Intent and interruption policy

The feature responds to explicit learning-status and vocabulary-profile questions. Normal English conversation remains unchanged and is not interrupted by a large recommendation card. The intent matcher supports common Chinese and English phrases and runs before the online-model branch so these queries are deterministic and available offline.

## Architecture

### Learning advisor service

Add a focused `LearningAdvisorService` in the agent domain. It reads current-user-scoped data through the existing mappers and `UserContext` and exposes two operations:

- `dailyRoute(includeCurrentExchange)` returns the daily completion snapshot and navigation actions. The chat path passes `true`; standalone reads pass `false`.
- `vocabularyProfile(group, page, size)` returns the requested mastered or unfamiliar page.

Date boundaries are calculated once per request as `[todayStart, tomorrowStart)` using the server's configured local timezone. Every mapper query includes the current user ID.

The service owns progress calculations and vocabulary classification. `AgentOrchestrator` owns intent selection and wording, while the frontend owns rendering and navigation. This keeps business rules out of prompts and Vue components.

### Agent integration

Extend the Agent intent set with daily-progress and vocabulary-profile intents. For these intents, `AgentOrchestrator`:

1. Records Observe and Think trace steps as it does today.
2. Calls `LearningAdvisorService` and records the call in the Act trace.
3. Builds a concise local reply from the snapshot.
4. Returns a structured learning card and route actions in `AgentResult`.
5. Records the final Answer trace.

Existing word lookup, news, essay, RAG, and general chat branches keep their current behaviour and fallback paths.

### API contracts

Extend `ChatResponse` with optional structured fields while preserving all existing fields:

```text
learningCard?: {
  type: DAILY_ROUTE | VOCABULARY_PROFILE
  title: string
  summary: string
  group?: UNFAMILIAR | MASTERED
  items: [{ key, label, detail, status }]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
actions: [{ id, label, section, route, query }]
```

`section` is restricted to existing application sections. `route` is selected server-side from a fixed allowlist, never copied from model output or user input.

Add a read-only endpoint for card pagination:

```text
GET /api/learning-advisor/vocabulary?group=unfamiliar|mastered&page=0&size=10
```

It returns the same vocabulary-profile card shape used by chat. Invalid group or pagination input produces the existing concise `IllegalArgumentException` response. Authentication and current-user isolation follow the existing `/api/**` interceptor behaviour.

### Frontend integration

Assistant messages may carry `learningCard` and `actions`. The chat message renderer adds a compact structured block below the assistant text:

- Daily progress uses a plain vertical task list with completion state and route buttons.
- Vocabulary profile uses a dense word list, count summary, pagination controls, and one primary module action.
- Buttons call the existing Vue Router navigation path. A query such as `mode=mastered` is applied when a specific sub-view is required.

The visual thesis is “a calm learning checklist embedded in the conversation”: existing typography and surfaces, one green action accent, no separate dashboard-style card grid. Interaction is limited to a short card entrance, clear button hover/press feedback, and a restrained page-content transition. Reduced-motion preferences disable nonessential motion.

The latest response gets its structured card immediately. When a conversation is reloaded, ordinary message text remains available under the existing history contract; persisting and replaying historical interactive cards is not part of this version.

## Data Details

Daily completion is derived from existing records:

- Conversation: assistant and user messages in a user-owned conversation during the day; the current request counts only after its assistant message has been saved.
- Vocabulary: `word_review_log` rows with `review_type = 'QUIZ'` during the day.
- Review: no current progress row has an outstanding wrong count. This matches the existing `ReviewPlannerTool.planToday` queue, which currently includes all wrong words regardless of `next_review_at`.
- Writing: `essay_submission.created_at` during the day. A submission exists only after the review operation completes and persists it.
- Reading: a current-user `reading_record` with `finished = true` and `updated_at` during the day.

Because `ChatService` persists the assistant message after `AgentOrchestrator` returns, the progress query cannot see that row yet. The orchestrator therefore calls `dailyRoute(true)`, which treats the in-flight request as one completed exchange in addition to persisted exchanges. This prevents “today's conversation is incomplete” in the reply that completes the exchange without changing message-save ordering.

## Error Handling

- Advisor calculation failures do not call the model to guess user data. They produce a concise local error reply and no unsafe action.
- A pagination request whose page is past the end returns the last valid page when results exist; empty sets return page zero.
- Missing word rows are skipped without failing the full page, and the total reflects renderable word entries.
- Navigation actions use fixed application routes, preventing arbitrary URL injection.

## Testing

Backend tests use the existing MockMvc integration-test style and unavailable AI base URL. Tests cover:

- Daily route with no activity, partial activity, and all five activities complete.
- The current completed conversation being reflected in its own advisor reply.
- Midnight boundaries using records before and within today's range.
- User isolation across route counts and vocabulary pages.
- Mastered and unfamiliar classification, including a familiar word with outstanding wrong answers.
- Vocabulary first page, next page, empty page, invalid group, and size clamping.
- Structured action routes and preservation of existing ChatResponse fields.
- Existing general-chat fallback and trace stages remaining unchanged.

Frontend verification covers:

- Rendering both card types inside assistant messages.
- Previous/next disabled states and page replacement without duplicating messages.
- Every action opening the intended section or review sub-view.
- Keyboard focus, readable mobile wrapping, and reduced-motion behaviour.
- A production frontend build followed by the full Maven test suite.

## Scope Boundaries

This version does not add embeddings, model-generated database queries, long-term learning-plan generation, historical interactive-card replay, notifications, or a new learning-path table. It derives recommendations from existing user-scoped records and fixed product rules.
