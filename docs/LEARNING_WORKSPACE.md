# Learning workspace update

The desktop writing workspace now supports local account-scoped drafts, live word count, linked sentence annotations, token-level revision comparison, writing history, and student rewrite/resubmission. Each rewrite references an owned submission. Scores are compared only when the review source, model, exam type and target level match; local scores are explicitly labeled as practice references.

Reading keeps dictionary lookup immediate. The user can request a separate AI interpretation of the selected word in its actual article sentence. Suggested phrases must occur verbatim in that sentence and contain the selected word. Failed model calls return clearly labeled general dictionary meanings. Saving records the source sentence, surface form, article and title in the current user's progress; it does not overwrite the shared dictionary example. Irregular forms are reviewed with an explicit original-form spelling label.

The reading discussion action passes an article ID rather than a large visible prompt. The server loads the article, keeps its reference in message metadata, and restores the material on later turns in the same owned conversation. Chat history renders a compact article attachment.

`/api/reviews/today` includes words whose next review time has arrived, including previously successful words. `/api/reviews/mistakes` supports voluntary early practice. Coach weak-word context continues to use the mistake queue. Failed review submissions retain answers and do not display a fabricated incorrect result.

Learning evidence uses `STRICT_REVIEW` logs from the current calendar day and six preceding days, compared with the preceding seven days. Accuracy includes retries and always shows sample counts; missing accuracy is null. Recurring words have at least two failed attempts in the recent period. Rewrite counts include submissions linked to an earlier owned essay. These metrics do not infer independent mastery or learning causality.

AI settings include a real chat-completion connection test against the current form without saving it. Saved settings are initially untested; model-list retrieval alone does not prove chat availability. The shared client has a 5-second connection timeout and 45-second read timeout. Replies and structured feedback arrive together; waiting indicators show actual elapsed time without simulated completion percentages. Existing local fallbacks remain available.

Database additions are declared in `schema.sql` and applied to existing installations by `DatabaseMigration`. No bulk dictionary import or destructive migration is involved.

Validation: full Maven test suite, frontend Node tests and Vite build, plus desktop browser checks at 1920×1080 and 1366×768 in light/dark themes. HTTP integration tests use a local OpenAI-compatible protocol fixture; they do not establish the quality or availability of an external provider.

## Personal learning journal

The sidebar's 学习档案 opens `/report` and reads the current user's `/api/report/journal`. Daily first-time words and cumulative words deduplicate actual QUIZ / STRICT_REVIEW word IDs; daily review words deduplicate strict-review words, while accuracy includes every strict-review attempt. Charts offer 7/30-day windows, the calendar shows 12 weeks, and the clickable tree connects recorded totals to practice, mastered words, reading and writing. Counts remain zero or unavailable when there are no records.

Login days are persisted in `user_login_day` on successful registration/login or authenticated-session visits, once per user and Beijing calendar day. Existing session visits after midnight are included. Historical logins are not inferred from registration age or study records. The journal separately shows membership duration, recorded login days, current/longest login streaks, and days with dated study records. Study events are practice attempts, essay submissions, user chat messages and first recorded article visits. They are not a time-spent estimate.

The journal uses locally bundled Rough.js (MIT) and Caveat (OFL) assets, loaded only when this page opens. Its strokes use stable seeds; chart lengths and bar heights are based on real values. Both light/dark desktop themes, keyboard navigation, reduced motion, error/retry and empty states are supported. References: [Rough.js](https://roughjs.com/), [handdrawn-diagrams skill](https://github.com/ongjin/handdrawn-diagrams/blob/main/skills/handdrawn-diagrams/SKILL.md).

## Administrator workspace

The desktop administrator pages share the learning workspace's green/blue palette, navigation frame, account menu and dark theme. The overview separates daily practice counts, the seven-day trend, account status and content inventory. Rough.js draws stable hand-drawn navigation icons and hatched columns; the chart uses a zero baseline, keeps zero values empty, and supports pointer and keyboard selection. Enabled accounts are explicitly labeled as account status rather than login activity.

User, vocabulary, knowledge and reading management use independently scrolling lists and detail/edit panels with visible form actions. Existing APIs, administrator restrictions and account-scoped model settings are preserved. Search empty states, initial-load retry and action failure messages retain entered form data. The administrator route and drawing code are loaded on demand.

## Interactive workspace illustrations

The home book turns on click or keyboard activation and displays a shuffled 32-word bilingual discovery deck. A deck does not repeat a word until exhausted, and a new deck or visit avoids immediately repeating the last page. These local previews do not count as quiz answers. Conversation bubbles place scene-specific starters or assistant questions in the composer, preserve user-authored text and never submit a message. The writing illustration cycles local expression examples without altering drafts or presenting them as model feedback. The reading lens toggles sentence focus using existing token sentence boundaries; dictionary lookup, source text and recorded reading progress retain their behavior.

Each illustration has distinct low-amplitude idle motion. Intersection and tab-visibility observers pause hidden animations; reduced-motion preferences remove idle and flip animations while keeping every action available. All illustrations are accessible buttons. No new network provider or external asset dependency is required.

## Authentication garden

The desktop login and registration pages share a fixed-height form area, sliding mode indicator, icon-led fields and the application's light/dark palette. A complete word-flower illustration reveals a different bilingual word on click or keyboard activation; its local discovery deck does not change learning records. Existing floating words retain hover meanings. Garden motion pauses while entering credentials, when hidden and under reduced-motion preferences. Password visibility, error feedback and student/administrator redirects retain the existing authentication APIs.

The authentication scene adds flowing ribbon edges, slowly drifting green/blue light, sparse fireflies, a drawn title underline and staggered form entrances. Bounded pointer depth interpolates only while moving and freezes over interactive words. Entering the form softens the scene and returns its layers to rest. Flower activation unfolds six petals and emits fading rings; repeated activation replaces the prior animation. Reduced-motion preferences and component teardown cancel the imperative animation and pointer frame callbacks.
