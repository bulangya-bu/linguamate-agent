package com.example.javabigwork;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.javabigwork.auth.LoginActivityService;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.report.service.LearningJournalService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class LearningCalendarTests {
    @Test
    void futureMonthContainsAllDatesWithZeroActivityWithoutQueryingTheDatabase() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UserContext user = mock(UserContext.class);
        when(user.userId()).thenReturn(1L);
        var service = new LearningJournalService(jdbc, user, null);
        YearMonth future = YearMonth.now(LoginActivityService.ZONE).plusMonths(1);

        var calendar = service.calendar(future.toString());

        assertEquals(future.toString(), calendar.month());
        assertEquals(LocalDate.now(LoginActivityService.ZONE), calendar.today());
        assertEquals(future.lengthOfMonth(), calendar.days().size());
        assertEquals(future.atDay(1), calendar.days().get(0).date());
        assertEquals(future.atEndOfMonth(), calendar.days().get(future.lengthOfMonth() - 1).date());
        calendar.days().forEach(day -> {
            assertEquals(0, day.studyEvents());
            assertEquals(0, day.newWords());
            assertEquals(0, day.reviewWords());
        });
        verifyNoInteractions(jdbc);
    }

    @Test
    void invalidMonthIsRejectedBeforeLookingUpData() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UserContext user = mock(UserContext.class);
        var service = new LearningJournalService(jdbc, user, null);

        for (String month : Arrays.asList(null, "", "2024-2", "2024-00", "2024-13", "0000-01", "2024-02-01")) {
            assertThrows(IllegalArgumentException.class, () -> service.calendar(month));
        }
        verifyNoInteractions(jdbc, user);
    }
}
