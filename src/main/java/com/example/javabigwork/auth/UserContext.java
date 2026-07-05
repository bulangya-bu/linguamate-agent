package com.example.javabigwork.auth;

import com.example.javabigwork.auth.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class UserContext {
    private final ThreadLocal<UserAccount> current = new ThreadLocal<>();

    public void set(UserAccount user) {
        current.set(user);
    }

    public void clear() {
        current.remove();
    }

    public UserAccount requireUser() {
        UserAccount user = current.get();
        if (user == null) {
            throw new AuthenticationRequiredException("请先登录");
        }
        return user;
    }

    public UserAccount currentUser() {
        return current.get();
    }

    public Long userId() {
        return requireUser().id;
    }

    public boolean isAdmin() {
        UserAccount user = current.get();
        return user != null && "ADMIN".equals(user.role);
    }
}
