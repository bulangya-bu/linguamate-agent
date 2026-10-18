package com.example.javabigwork.report.service;

import com.example.javabigwork.auth.LoginActivityService;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.auth.mapper.UserAccountMapper;
import com.example.javabigwork.report.dto.LearningJournalView;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LearningJournalService {
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
        Map<LocalDate, Counts> daily = new HashMap<>();
        jdbc.query("""
                SELECT DATE(first_seen) AS day, COUNT(*) AS total FROM
                  (SELECT word_id, MIN(created_at) AS first_seen FROM word_review_log
                   WHERE user_id=? AND review_type IN ('QUIZ','STRICT_REVIEW') AND created_at IS NOT NULL AND created_at<? GROUP BY word_id) firsts
                WHERE first_seen>=? GROUP BY DATE(first_seen)
                """, rs -> { counts(daily, rs.getDate("day").toLocalDate()).newWords = rs.getLong("total"); }, userId, today.plusDays(1), first);
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
                }, userId, first, today.plusDays(1));
        // Only dated, persisted study events are used; no inferred historical login days.
        Set<LocalDate> activeDates = new HashSet<>();
        jdbc.query("""
                SELECT day, COUNT(*) AS total FROM (
                    SELECT DATE(created_at) AS day FROM word_review_log WHERE user_id=? AND created_at IS NOT NULL
                    UNION ALL SELECT DATE(created_at) FROM essay_submission WHERE user_id=? AND created_at IS NOT NULL
                    UNION ALL SELECT DATE(m.created_at) FROM message m JOIN conversation c ON m.conversation_id=c.id
                      WHERE c.user_id=? AND m.role='user' AND m.created_at IS NOT NULL
                    UNION ALL SELECT DATE(created_at) FROM reading_record WHERE user_id=? AND created_at IS NOT NULL
                ) events WHERE day<=? GROUP BY day
                """, rs -> { LocalDate date = rs.getDate("day").toLocalDate(); activeDates.add(date);
                    if (!date.isBefore(first)) counts(daily, date).events = rs.getLong("total");
                }, userId, userId, userId, userId, today);
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
                scalar("SELECT COUNT(*) FROM message m JOIN conversation c ON m.conversation_id=c.id WHERE c.user_id=? AND m.role='user'", userId));
        List<LearningJournalView.Day> days = first.datesUntil(first.plusWeeks(12)).map(date -> {
            var value = daily.getOrDefault(date, new Counts());
            return new LearningJournalView.Day(date, value.newWords, value.reviews, value.quizzes, value.attempts,
                    rate(value.corrects, value.attempts), value.events, loginSet.contains(date));
        }).toList();
        return new LearningJournalView(today, LoginActivityService.ZONE.getId(), account.nickname, joined,
                Math.max(1, ChronoUnit.DAYS.between(joined, today) + 1), logins.isEmpty() ? null : logins.get(0),
                logins.size(), streak(loginSet, today), longestStreak(logins), activeDates.size(), totals, days);
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
    private static class Counts { long newWords, reviews, quizzes, attempts, corrects, events; }
    private Counts counts(Map<LocalDate, Counts> map, LocalDate date) { return map.computeIfAbsent(date, key -> new Counts()); }
    private long scalar(String sql, long userId) { Long value = jdbc.queryForObject(sql, Long.class, userId); return value == null ? 0 : value; }
    private long number(Map<String, Object> map, String key) { return ((Number) map.get(key)).longValue(); }
    private Long rate(long correct, long attempts) { return attempts == 0 ? null : Math.round(correct * 100.0 / attempts); }
}
