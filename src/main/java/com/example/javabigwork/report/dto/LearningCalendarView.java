package com.example.javabigwork.report.dto;

import java.time.LocalDate;
import java.util.List;

public record LearningCalendarView(String month, LocalDate today, List<Day> days) {
    public record Day(LocalDate date, long newWords, long reviewWords, long studyEvents,
                      long aiChatTurns, long aiEssayReviews, long readingsFinished) {}
}
