package com.example.javabigwork.auth;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LoginActivityService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final String SESSION_DAY = "LINGUAMATE_RECORDED_LOGIN_DAY";
    private final JdbcTemplate jdbc;

    public LoginActivityService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void recordVisit(Long userId, HttpSession session) {
        LocalDate date = LocalDate.now(ZONE);
        String key = userId + ":" + date;
        if (key.equals(session.getAttribute(SESSION_DAY))) return;
        jdbc.update("""
                INSERT INTO user_login_day(user_id, login_date, first_seen_at) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE user_id = VALUES(user_id)
                """, userId, date, java.time.LocalDateTime.now(ZONE).withNano(0));
        session.setAttribute(SESSION_DAY, key);
    }
}
