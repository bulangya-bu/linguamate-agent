package com.example.javabigwork.report.service;

import com.example.javabigwork.auth.LoginActivityService;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.auth.mapper.UserAccountMapper;
import com.example.javabigwork.report.dto.LearningCalendarView;
import com.example.javabigwork.report.dto.LearningJournalView;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LearningJournalService {
    private static final Pattern CALENDAR_MONTH = Pattern.compile("(?!0000)[0-9]{4}-(0[1-9]|1[0-2])");
    private static final Pattern ENGLISH_WORD = Pattern.compile("[A-Za-z]+(?:['’-][A-Za-z]+)*");
    private final JdbcTemplate jdbc;
    private final UserContext userContext;
    private final UserAccountMapper users;

    public LearningJournalService(JdbcTemplate jdbc, UserContext userContext, UserAccountMapper users) {
        this.jdbc = jdbc; this.userContext = userContext; this.users = users;
    }

    public LearningJournalView journal() {
        long userId = userContext.userId();
        var account = users.selectById(userId);
        LocalDate today = LocalDate.now(LoginActivityService.ZONE);
        LocalDate joined = account.createdAt == null ? today : account.createdAt.toLocalDate();
        LocalDate first = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(11);
        List<LocalDate> logins = jdbc.query("SELECT login_date FROM user_login_day WHERE user_id=? AND login_date<=? ORDER BY login_date",
                (rs, row) -> rs.getDate(1).toLocalDate(), userId, today);
        Set<LocalDate> loginSet = new HashSet<>(logins);
        Map<LocalDate, Counts> daily = loadDailyCounts(userId, first, today.plusDays(1));
        Map<LocalDate, Long> activity = loadStudyEvents(userId, null, today.plusDays(1));
        Map<String, Object> wordTotals = jdbc.queryForMap("""
                SELECT COUNT(DISTINCT CASE WHEN review_type IN ('QUIZ','STRICT_REVIEW') THEN word_id END) AS studied,
                  COUNT(DISTINCT CASE WHEN review_type='STRICT_REVIEW' THEN word_id END) AS reviewed,
                  COUNT(CASE WHEN review_type='QUIZ' THEN 1 END) AS quizzes,
                  COUNT(CASE WHEN review_type='STRICT_REVIEW' THEN 1 END) AS attempts,
                  COUNT(CASE WHEN review_type='STRICT_REVIEW' AND correct=TRUE THEN 1 END) AS corrects
                FROM word_review_log WHERE user_id=? AND created_at<?
                """, userId, today.plusDays(1));
        long attempts = number(wordTotals, "attempts");
        var totals = new LearningJournalView.Totals(number(wordTotals, "studied"), number(wordTotals, "reviewed"),
                scalar("SELECT COUNT(*) FROM user_word_progress WHERE user_id=? AND familiarity>=3 AND COALESCE(wrong_count,0)=0", userId),
                number(wordTotals, "quizzes"), attempts, rate(number(wordTotals, "corrects"), attempts),
                scalar("SELECT COUNT(*) FROM essay_submission WHERE user_id=?", userId),
                scalar("SELECT COUNT(*) FROM reading_record WHERE user_id=? AND finished=TRUE", userId),
                scalar("SELECT COUNT(*) FROM message m JOIN conversation c ON m.conversation_id=c.id WHERE c.user_id=? AND m.role='user'", userId),
                sumEnglishWords("""
                        SELECT a.content
                        FROM news_article a
                        WHERE EXISTS (
                            SELECT 1 FROM reading_record r
                            WHERE r.article_id=a.id AND r.user_id=? AND r.finished=TRUE
                        )
                        """, userId),
                sumEnglishWords("SELECT original_text FROM essay_submission WHERE user_id=?", userId));
        return new LearningJournalView(today, LoginActivityService.ZONE.getId(), account.nickname, joined,
                Math.max(1, ChronoUnit.DAYS.between(joined, today) + 1), logins.isEmpty() ? null : logins.get(0),
                logins.size(), streak(loginSet, today), longestStreak(logins), activity.size(), totals,
                days(first, first.plusWeeks(12), daily, activity, loginSet));
    }

    public LearningCalendarView calendar(String month) {
        if (month == null || !CALENDAR_MONTH.matcher(month).matches()) {
            throw new IllegalArgumentException("月份格式无效，请使用 YYYY-MM");
        }
        long userId = userContext.userId();
        YearMonth selected = YearMonth.parse(month);
        LocalDate today = LocalDate.now(LoginActivityService.ZONE);
        LocalDate first = selected.atDay(1);
        LocalDate afterMonth = first.plusMonths(1);
        LocalDate afterToday = today.plusDays(1);
        LocalDate end = afterMonth.isBefore(afterToday) ? afterMonth : afterToday;
        Map<LocalDate, Counts> daily = first.isBefore(end) ? loadDailyCounts(userId, first, end) : Map.of();
        Map<LocalDate, Long> activity = first.isBefore(end) ? loadStudyEvents(userId, first, end) : Map.of();
        var monthDays = first.datesUntil(afterMonth).map(date -> {
            Counts value = daily.getOrDefault(date, new Counts());
            return new LearningCalendarView.Day(date, value.newWords, value.reviews, activity.getOrDefault(date, 0L));
        }).toList();
        return new LearningCalendarView(month, today, monthDays);
    }

    private List<LearningJournalView.Day> days(LocalDate first, LocalDate end, Map<LocalDate, Counts> daily,
            Map<LocalDate, Long> activity, Set<LocalDate> logins) {
        return first.datesUntil(end).map(date -> {
            var value = daily.getOrDefault(date, new Counts());
            return new LearningJournalView.Day(date, value.newWords, value.reviews, value.quizzes, value.attempts,
                    rate(value.corrects, value.attempts), activity.getOrDefault(date, 0L), logins.contains(date),
                    value.aiChatTurns, value.aiEssayReviews, value.readingsFinished);
        }).toList();
    }

    private Map<LocalDate, Counts> loadDailyCounts(long userId, LocalDate first, LocalDate end) {
        Map<LocalDate, Counts> daily = new HashMap<>();
        jdbc.query("""
                SELECT DATE(first_seen) AS day, COUNT(*) AS total FROM
                  (SELECT word_id, MIN(created_at) AS first_seen FROM word_review_log
                   WHERE user_id=? AND review_type IN ('QUIZ','STRICT_REVIEW') AND created_at IS NOT NULL AND created_at<? GROUP BY word_id) firsts
                WHERE first_seen>=? GROUP BY DATE(first_seen)
                """, rs -> { counts(daily, rs.getDate("day").toLocalDate()).newWords = rs.getLong("total"); }, userId, end, first);
        jdbc.query("""
                SELECT DATE(created_at) AS day,
                  COUNT(CASE WHEN review_type='QUIZ' THEN 1 END) AS quizzes,
                  COUNT(DISTINCT CASE WHEN review_type='STRICT_REVIEW' THEN word_id END) AS reviews,
                  COUNT(CASE WHEN review_type='STRICT_REVIEW' THEN 1 END) AS attempts,
                  COUNT(CASE WHEN review_type='STRICT_REVIEW' AND correct=TRUE THEN 1 END) AS corrects
                FROM word_review_log WHERE user_id=? AND created_at>=? AND created_at<? GROUP BY DATE(created_at)
                """, rs -> { var item = counts(daily, rs.getDate("day").toLocalDate());
                    item.quizzes = rs.getLong("quizzes"); item.reviews = rs.getLong("reviews");
                    item.attempts = rs.getLong("attempts"); item.corrects = rs.getLong("corrects");
                }, userId, first, end);
        jdbc.query("""
                SELECT DATE(event_time) AS day,
                  COUNT(CASE WHEN kind='chat' THEN 1 END) AS chat_turns,
                  COUNT(CASE WHEN kind='writing' THEN 1 END) AS essay_reviews,
                  COUNT(CASE WHEN kind='reading' THEN 1 END) AS readings_finished
                FROM (
                    SELECT m.created_at AS event_time, 'chat' AS kind
                    FROM message m JOIN conversation c ON m.conversation_id=c.id
                    WHERE c.user_id=? AND m.role='assistant'
                      AND JSON_UNQUOTE(JSON_EXTRACT(
                        CASE WHEN JSON_VALID(m.metadata) THEN m.metadata END, '$.coaching.source'
                      ))='model'
                    UNION ALL SELECT created_at, 'writing' FROM essay_submission WHERE user_id=? AND review_source='AI批改'
                    UNION ALL SELECT finished_at, 'reading' FROM reading_record WHERE user_id=? AND finished=TRUE
                ) task_events WHERE event_time>=? AND event_time<? GROUP BY DATE(event_time)
                """, rs -> {
                    var item = counts(daily, rs.getDate("day").toLocalDate());
                    item.aiChatTurns = rs.getLong("chat_turns");
                    item.aiEssayReviews = rs.getLong("essay_reviews");
                    item.readingsFinished = rs.getLong("readings_finished");
                }, userId, userId, userId, first, end);
        return daily;
    }

    // The main journal counts all active dates, while the calendar only needs days in the requested month.
    private Map<LocalDate, Long> loadStudyEvents(long userId, LocalDate first, LocalDate end) {
        String sql = """
                SELECT day, COUNT(*) AS total FROM (
                    SELECT DATE(created_at) AS day FROM word_review_log WHERE user_id=? AND created_at IS NOT NULL
                    UNION ALL SELECT DATE(created_at) FROM essay_submission WHERE user_id=? AND created_at IS NOT NULL
                    UNION ALL SELECT DATE(m.created_at) FROM message m JOIN conversation c ON m.conversation_id=c.id
                      WHERE c.user_id=? AND m.role='user' AND m.created_at IS NOT NULL
                    UNION ALL SELECT DATE(created_at) FROM reading_record WHERE user_id=? AND created_at IS NOT NULL
                ) events WHERE day<?
                """ + (first == null ? "" : " AND day>=?") + " GROUP BY day";
        Object[] parameters = first == null
                ? new Object[]{userId, userId, userId, userId, end}
                : new Object[]{userId, userId, userId, userId, end, first};
        Map<LocalDate, Long> activity = new HashMap<>();
        jdbc.query(sql, rs -> {
            activity.put(rs.getDate("day").toLocalDate(), rs.getLong("total"));
        }, parameters);
        return activity;
    }

    public static int streak(Set<LocalDate> days, LocalDate today) {
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int count = 0;
        while (days.contains(cursor)) { count++; cursor = cursor.minusDays(1); }
        return count;
    }
    public static int longestStreak(List<LocalDate> dates) {
        List<LocalDate> sorted = dates.stream().distinct().sorted().toList();
        int best = 0, current = 0; LocalDate previous = null;
        for (LocalDate date : sorted) {
            current = previous != null && previous.plusDays(1).equals(date) ? current + 1 : 1;
            best = Math.max(best, current); previous = date;
        }
        return best;
    }
    private static class Counts {
        long newWords, reviews, quizzes, attempts, corrects;
        long aiChatTurns, aiEssayReviews, readingsFinished;
    }
    private Counts counts(Map<LocalDate, Counts> map, LocalDate date) { return map.computeIfAbsent(date, key -> new Counts()); }
    private long scalar(String sql, long userId) { Long value = jdbc.queryForObject(sql, Long.class, userId); return value == null ? 0 : value; }
    private long sumEnglishWords(String sql, long userId) {
        return jdbc.query(sql, results -> {
            long total = 0;
            while (results.next()) {
                String text = results.getString(1);
                if (text != null) {
                    total += ENGLISH_WORD.matcher(text).results().count();
                }
            }
            return total;
        }, userId);
    }
    private long number(Map<String, Object> map, String key) { return ((Number) map.get(key)).longValue(); }
    private Long rate(long correct, long attempts) { return attempts == 0 ? null : Math.round(correct * 100.0 / attempts); }
}
