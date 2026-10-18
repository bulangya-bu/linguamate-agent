package com.example.javabigwork.config;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DatabaseMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_login_day (
                    user_id BIGINT NOT NULL,
                    login_date DATE NOT NULL,
                    first_seen_at TIMESTAMP NOT NULL,
                    PRIMARY KEY (user_id, login_date)
                )
                """);
        List<String> userOwnedTables = List.of(
                "conversation",
                "ai_setting",
                "user_word_progress",
                "word_review_log",
                "essay_submission",
                "reading_record",
                "user_level"
        );
        userOwnedTables.forEach(table -> addColumnIfMissing(table, "user_id", "BIGINT NOT NULL DEFAULT 1"));
        addColumnIfMissing("reading_record", "updated_at", "TIMESTAMP");
        addColumnIfMissing("word_review_log", "spelling_answer", "VARCHAR(255)");
        addColumnIfMissing("word_review_log", "meaning_answer", "VARCHAR(255)");
        addColumnIfMissing("word_review_log", "spelling_correct", "BOOLEAN");
        addColumnIfMissing("word_review_log", "meaning_correct", "BOOLEAN");
        addColumnIfMissing("word_review_log", "review_type", "VARCHAR(32)");
        addColumnIfMissing("user_word_progress", "source_sentence", "TEXT");
        addColumnIfMissing("user_word_progress", "source_title", "VARCHAR(255)");
        addColumnIfMissing("user_word_progress", "source_word", "VARCHAR(64)");
        addColumnIfMissing("user_word_progress", "source_article_id", "BIGINT");
        addColumnIfMissing("essay_submission", "revision_of_id", "BIGINT");
        addColumnIfMissing("essay_submission", "review_source", "VARCHAR(64)");
        addColumnIfMissing("essay_submission", "model_name", "VARCHAR(128)");
        addColumnIfMissing("essay_submission", "target_level", "VARCHAR(64)");

        backfillReadingRecordUpdatedAt();
        mergeDuplicateReadingRecords();
        mergeDuplicateUserWordProgress();

        addUniqueIndexIfMissing("ai_setting", "uk_ai_setting_user", "user_id");
        addUniqueIndexIfMissing("reading_record", "uk_reading_record_user_article", "user_id", "article_id");
        addUniqueIndexIfMissing("user_word_progress", "uk_user_word_progress_user_word", "user_id", "word_id");
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
    }

    private void addUniqueIndexIfMissing(String tableName, String indexName, String... columnNames) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND INDEX_NAME = ?
                """,
                Integer.class,
                tableName,
                indexName
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + tableName
                + " ADD UNIQUE KEY " + indexName
                + " (" + String.join(", ", columnNames) + ")");
    }

    private void backfillReadingRecordUpdatedAt() {
        jdbcTemplate.execute("""
                UPDATE reading_record
                SET updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
                WHERE updated_at IS NULL
                """);
    }

    private void mergeDuplicateReadingRecords() {
        jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS reading_record_merge");
        jdbcTemplate.execute("""
                CREATE TEMPORARY TABLE reading_record_merge AS
                SELECT
                    MIN(id) AS keep_id,
                    user_id,
                    article_id,
                    MAX(COALESCE(read_progress, 0)) AS read_progress,
                    SUM(COALESCE(looked_up_words, 0)) AS looked_up_words,
                    MAX(CASE WHEN COALESCE(finished, FALSE) THEN 1 ELSE 0 END) AS finished,
                    MIN(created_at) AS created_at,
                    MAX(COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)) AS updated_at
                FROM reading_record
                WHERE article_id IS NOT NULL
                GROUP BY user_id, article_id
                HAVING COUNT(*) > 1
                """);
        jdbcTemplate.execute("""
                UPDATE reading_record rr
                JOIN reading_record_merge merged ON rr.id = merged.keep_id
                SET rr.read_progress = merged.read_progress,
                    rr.looked_up_words = merged.looked_up_words,
                    rr.finished = merged.finished = 1,
                    rr.created_at = COALESCE(merged.created_at, rr.created_at),
                    rr.updated_at = merged.updated_at
                """);
        jdbcTemplate.execute("""
                DELETE rr
                FROM reading_record rr
                JOIN reading_record_merge merged
                  ON rr.user_id = merged.user_id
                 AND rr.article_id = merged.article_id
                WHERE rr.id <> merged.keep_id
                """);
        jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS reading_record_merge");
    }

    private void mergeDuplicateUserWordProgress() {
        jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS user_word_progress_merge");
        jdbcTemplate.execute("""
                CREATE TEMPORARY TABLE user_word_progress_merge AS
                SELECT
                    MIN(id) AS keep_id,
                    user_id,
                    word_id,
                    MAX(COALESCE(familiarity, 0)) AS familiarity,
                    MAX(COALESCE(wrong_count, 0)) AS wrong_count,
                    SUM(COALESCE(review_count, 0)) AS review_count,
                    MAX(CASE WHEN COALESCE(favorite, FALSE) THEN 1 ELSE 0 END) AS favorite,
                    MIN(next_review_at) AS next_review_at,
                    MAX(last_review_at) AS last_review_at
                FROM user_word_progress
                WHERE word_id IS NOT NULL
                GROUP BY user_id, word_id
                HAVING COUNT(*) > 1
                """);
        jdbcTemplate.execute("""
                UPDATE user_word_progress progress
                JOIN user_word_progress_merge merged ON progress.id = merged.keep_id
                SET progress.familiarity = merged.familiarity,
                    progress.wrong_count = merged.wrong_count,
                    progress.review_count = merged.review_count,
                    progress.favorite = merged.favorite = 1,
                    progress.next_review_at = merged.next_review_at,
                    progress.last_review_at = merged.last_review_at
                """);
        jdbcTemplate.execute("""
                DELETE progress
                FROM user_word_progress progress
                JOIN user_word_progress_merge merged
                  ON progress.user_id = merged.user_id
                 AND progress.word_id = merged.word_id
                WHERE progress.id <> merged.keep_id
                """);
        jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS user_word_progress_merge");
    }
}
