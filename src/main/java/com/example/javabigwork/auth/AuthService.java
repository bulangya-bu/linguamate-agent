package com.example.javabigwork.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.auth.dto.AuthUserView;
import com.example.javabigwork.auth.dto.LoginRequest;
import com.example.javabigwork.auth.dto.RegisterRequest;
import com.example.javabigwork.auth.entity.UserAccount;
import com.example.javabigwork.auth.mapper.UserAccountMapper;
import com.example.javabigwork.cache.RedisCacheService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Order(2)
public class AuthService implements ApplicationRunner {
    public static final String SESSION_USER_ID = "LINGUAMATE_USER_ID";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String ACTIVE = "ACTIVE";
    private static final String DISABLED = "DISABLED";
    private static final Duration SESSION_USER_TTL = Duration.ofMinutes(30);

    private final UserAccountMapper userMapper;
    private final PasswordHasher passwordHasher;
    private final RedisCacheService redisCache;

    public AuthService(UserAccountMapper userMapper, PasswordHasher passwordHasher, RedisCacheService redisCache) {
        this.userMapper = userMapper;
        this.passwordHasher = passwordHasher;
        this.redisCache = redisCache;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureSoleAdmin();
        ensureAccount("student", "student123", "学习者", "USER");
    }

    @Transactional
    public AuthUserView register(RegisterRequest request, HttpServletRequest servletRequest) {
        String username = normalizeUsername(request.username());
        if (findByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        UserAccount user = new UserAccount();
        user.username = username;
        user.passwordHash = passwordHasher.hash(request.password());
        user.nickname = safeNickname(request.nickname(), username);
        user.role = "USER";
        user.status = ACTIVE;
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();
        userMapper.insert(user);
        servletRequest.getSession(true).setAttribute(SESSION_USER_ID, user.id);
        cacheSessionUser(user);
        return AuthUserView.from(user);
    }

    public AuthUserView login(LoginRequest request, HttpServletRequest servletRequest) {
        UserAccount user = findByUsername(normalizeUsername(request.username()));
        if (user == null || !passwordHasher.matches(request.password(), user.passwordHash)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (DISABLED.equals(user.status)) {
            throw new AuthorizationException("账号已被禁用");
        }
        servletRequest.getSession(true).setAttribute(SESSION_USER_ID, user.id);
        cacheSessionUser(user);
        return AuthUserView.from(user);
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object userId = session.getAttribute(SESSION_USER_ID);
            if (userId instanceof Long id) {
                evictSessionUser(id);
            }
            session.invalidate();
        }
    }

    public AuthUserView me(HttpServletRequest request) {
        UserAccount user = loadFromSession(request);
        return user == null ? null : AuthUserView.from(user);
    }

    public UserAccount loadFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_USER_ID);
        if (!(value instanceof Long userId)) {
            return null;
        }
        AuthUserView cached = redisCache.get(sessionUserKey(userId), AuthUserView.class).orElse(null);
        if (cached != null) {
            if (DISABLED.equals(cached.status())) {
                evictSessionUser(userId);
                session.invalidate();
                return null;
            }
            redisCache.put(sessionUserKey(userId), cached, SESSION_USER_TTL);
            return toUserAccount(cached);
        }
        UserAccount user = userMapper.selectById(userId);
        if (user == null || DISABLED.equals(user.status)) {
            evictSessionUser(userId);
            session.invalidate();
            return null;
        }
        cacheSessionUser(user);
        return user;
    }

    public UserAccount findByUsername(String username) {
        return userMapper.selectList(Wrappers.<UserAccount>query()
                        .eq("username", normalizeUsername(username))
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public void updateStatus(Long userId, String status) {
        UserAccount user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!ACTIVE.equals(normalized) && !DISABLED.equals(normalized)) {
            throw new IllegalArgumentException("状态只支持 ACTIVE 或 DISABLED");
        }
        user.status = normalized;
        user.updatedAt = LocalDateTime.now();
        userMapper.updateById(user);
        evictSessionUser(userId);
    }

    @Transactional
    public void updateNickname(Long userId, String nickname) {
        UserAccount user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (nickname != null && nickname.length() > 32) {
            throw new IllegalArgumentException("昵称最多 32 位");
        }
        user.nickname = safeNickname(nickname, user.username);
        user.updatedAt = LocalDateTime.now();
        userMapper.updateById(user);
        evictSessionUser(userId);
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 64) {
            throw new IllegalArgumentException("新密码长度应为 6-64 位");
        }
        UserAccount user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.passwordHash = passwordHasher.hash(newPassword);
        user.updatedAt = LocalDateTime.now();
        userMapper.updateById(user);
        evictSessionUser(userId);
    }

    private void cacheSessionUser(UserAccount user) {
        if (user == null || user.id == null) {
            return;
        }
        redisCache.put(sessionUserKey(user.id), AuthUserView.from(user), SESSION_USER_TTL);
    }

    private void evictSessionUser(Long userId) {
        redisCache.evict(sessionUserKey(userId));
    }

    private String sessionUserKey(Long userId) {
        return redisCache.key("session:user", userId);
    }

    private UserAccount toUserAccount(AuthUserView view) {
        UserAccount user = new UserAccount();
        user.id = view.id();
        user.username = view.username();
        user.nickname = view.nickname();
        user.role = view.role();
        user.status = view.status();
        return user;
    }

    private void ensureSoleAdmin() {
        UserAccount admin = findByUsername(ADMIN_USERNAME);
        if (admin == null) {
            ensureAccount(ADMIN_USERNAME, ADMIN_PASSWORD, "管理员", "ADMIN");
        } else {
            admin.passwordHash = passwordHasher.hash(ADMIN_PASSWORD);
            admin.nickname = safeNickname(admin.nickname, ADMIN_USERNAME);
            admin.role = "ADMIN";
            admin.status = ACTIVE;
            admin.updatedAt = LocalDateTime.now();
            if (admin.createdAt == null) {
                admin.createdAt = LocalDateTime.now();
            }
            userMapper.updateById(admin);
        }
        userMapper.selectList(Wrappers.<UserAccount>query()
                        .eq("role", "ADMIN")
                        .ne("username", ADMIN_USERNAME))
                .forEach(user -> {
                    user.role = "USER";
                    if (user.status == null || user.status.isBlank()) {
                        user.status = ACTIVE;
                    }
                    user.updatedAt = LocalDateTime.now();
                    userMapper.updateById(user);
                });
    }

    private void ensureAccount(String username, String password, String nickname, String role) {
        if (findByUsername(username) != null) {
            return;
        }
        UserAccount user = new UserAccount();
        user.username = username;
        user.passwordHash = passwordHasher.hash(password);
        user.nickname = nickname;
        user.role = role;
        user.status = ACTIVE;
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();
        userMapper.insert(user);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private String safeNickname(String nickname, String username) {
        return nickname == null || nickname.isBlank() ? username : nickname.trim();
    }
}
