package com.example.javabigwork.auth.dto;

import com.example.javabigwork.auth.entity.UserAccount;

public record AuthUserView(
        Long id,
        String username,
        String nickname,
        String role,
        String status
) {
    public static AuthUserView from(UserAccount user) {
        return new AuthUserView(user.id, user.username, user.nickname, user.role, user.status);
    }
}
