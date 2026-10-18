package com.example.javabigwork.report.dto;

import java.time.LocalDate;
import java.util.List;

public record LearningJournalView(LocalDate today, String timeZone, String nickname, LocalDate joinedOn,
        long membershipDays, LocalDate loginRecordedSince, long loginDays, int currentStreak, int longestStreak,
        int activeDays, Totals totals, List<Day> days) {
    public record Totals(long studiedWords, long reviewWords, long masteredWords, long quizAttempts,
                         long reviewAttempts, Long reviewAccuracy, long essays, long readings, long chatTurns) {}
    public record Day(LocalDate date, long newWords, long reviewWords, long quizAttempts, long reviewAttempts,
                      Long reviewAccuracy, long studyEvents, boolean loggedIn) {}
}
