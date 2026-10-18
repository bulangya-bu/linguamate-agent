# Learning workspace update

The desktop writing workspace now supports local account-scoped drafts, live word count, linked sentence annotations, token-level revision comparison, writing history, and student rewrite/resubmission. Each rewrite references an owned submission. Scores are compared only when the review source, model, exam type and target level match; local scores are explicitly labeled as practice references.

Reading keeps dictionary lookup immediate. The user can request a separate AI interpretation of the selected word in its actual article sentence. Suggested phrases must occur verbatim in that sentence and contain the selected word. Failed model calls return clearly labeled general dictionary meanings. Saving records the source sentence, surface form, article and title in the current user's progress; it does not overwrite the shared dictionary example. Irregular forms are reviewed with an explicit original-form spelling label.

The reading discussion action passes an article ID rather than a large visible prompt. The server loads the article, keeps its reference in message metadata, and restores the material on later turns in the same owned conversation. Chat history renders a compact article attachment.

`/api/reviews/today` includes words whose next review time has arrived, including previously successful words. `/api/reviews/mistakes` supports voluntary early practice. Coach weak-word context continues to use the mistake queue. Failed review submissions retain answers and do not display a fabricated incorrect result.

Learning evidence uses `STRICT_REVIEW` logs from the current calendar day and six preceding days, compared with the preceding seven days. Accuracy includes retries and always shows sample counts; missing accuracy is null. Recurring words have at least two failed attempts in the recent period. Rewrite counts include submissions linked to an earlier owned essay. These metrics do not infer independent mastery or learning causality.

AI settings include a real chat-completion connection test against the current form without saving it. Saved settings are initially untested; model-list retrieval alone does not prove chat availability. The shared client has a 5-second connection timeout and 45-second read timeout. Replies and structured feedback arrive together; waiting indicators show actual elapsed time without simulated completion percentages. Existing local fallbacks remain available.

Database additions are declared in `schema.sql` and applied to existing installations by `DatabaseMigration`. No bulk dictionary import or destructive migration is involved.

Validation: full Maven test suite, frontend Node tests and Vite build, plus desktop browser checks at 1920×1080 and 1366×768 in light/dark themes. HTTP integration tests use a local OpenAI-compatible protocol fixture; they do not establish the quality or availability of an external provider.
