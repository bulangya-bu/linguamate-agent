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
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Order(2)
public class AuthService implements ApplicationRunner {
    public static final String SESSION_USER_ID = "LINGUAMATE_USER_ID";
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String ADMIN_USERNAME = "admin";
    private static final String LEGACY_ADMIN_PASSWORD = "admin123";
    private static final String ACTIVE = "ACTIVE";
    private static final String DISABLED = "DISABLED";
    private static final Duration SESSION_USER_TTL = Duration.ofMinutes(30);

    private final UserAccountMapper userMapper;
    private final PasswordHasher passwordHasher;
    private final RedisCacheService redisCache;
    private final LoginActivityService loginActivity;
    private final String adminBootstrapPassword;

    public AuthService(
            UserAccountMapper userMapper,
            PasswordHasher passwordHasher,
            RedisCacheService redisCache,
            LoginActivityService loginActivity,
            @Value("${LINGUAMATE_ADMIN_BOOTSTRAP_PASSWORD:}") String adminBootstrapPassword
    ) {
        this.userMapper = userMapper;
        this.passwordHasher = passwordHasher;
        this.redisCache = redisCache;
        this.loginActivity = loginActivity;
        this.adminBootstrapPassword = adminBootstrapPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        configureAdmin();
        ensureAccount("student", "student123", "学习者", "USER");
    }

    @Transactional
    public AuthUserView register(RegisterRequest request, HttpServletRequest servletRequest) {
        String username = normalizeUsername(request.username());
        if (username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (username.length() < 3 || username.length() > 32) {
            throw new IllegalArgumentException("用户名长度应为 3-32 位");
        }
        if (ADMIN_USERNAME.equals(username) || findByUsername(username) != null) {
            throw new UsernameAlreadyExistsException();
        }
        UserAccount user = new UserAccount();
        user.username = username;
        user.passwordHash = passwordHasher.hash(request.password());
        user.nickname = safeNickname(request.nickname(), username);
        user.role = "USER";
        user.status = ACTIVE;
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new UsernameAlreadyExistsException();
        }
        return startSession(user, servletRequest);
    }

    public AuthUserView login(LoginRequest request, HttpServletRequest servletRequest) {
        UserAccount user = findByUsername(normalizeUsername(request.username()));
        if (user == null || !passwordHasher.matches(request.password(), user.passwordHash)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (DISABLED.equals(user.status)) {
            throw new AuthorizationException("账号已被禁用");
        }
        return startSession(user, servletRequest);
    }

    private AuthUserView startSession(UserAccount user, HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(SESSION_USER_ID, user.id);
        loginActivity.recordVisit(user.id, session);
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
            loginActivity.recordVisit(userId, session);
            return toUserAccount(cached);
        }
        UserAccount user = userMapper.selectById(userId);
        if (user == null || DISABLED.equals(user.status)) {
            evictSessionUser(userId);
            session.invalidate();
            return null;
        }
        cacheSessionUser(user);
        loginActivity.recordVisit(userId, session);
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

    private void configureAdmin() {
        if (adminBootstrapPassword.isBlank()) {
            return;
        }
        UserAccount admin = findByUsername(ADMIN_USERNAME);
        if (admin == null) {
            validateAdminBootstrapPassword();
            ensureAccount(ADMIN_USERNAME, adminBootstrapPassword, "管理员", "ADMIN");
            return;
        }
        if (!"ADMIN".equals(admin.role)) {
            log.warn("Skipping configured admin bootstrap because the username is already in use");
            return;
        }
        if (!passwordHasher.matches(LEGACY_ADMIN_PASSWORD, admin.passwordHash)) {
            return;
        }
        validateAdminBootstrapPassword();
        userMapper.update(null, Wrappers.<UserAccount>update()
                .eq("id", admin.id)
                .eq("role", "ADMIN")
                .eq("password_hash", admin.passwordHash)
                .set("password_hash", passwordHasher.hash(adminBootstrapPassword))
                .set("updated_at", LocalDateTime.now()));
    }

    private void validateAdminBootstrapPassword() {
        if (adminBootstrapPassword.length() < 12 || adminBootstrapPassword.length() > 64
                || LEGACY_ADMIN_PASSWORD.equals(adminBootstrapPassword)) {
            throw new IllegalStateException("LINGUAMATE_ADMIN_BOOTSTRAP_PASSWORD must be 12–64 characters "
                    + "and differ from the legacy demo password");
        }
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
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            // Another instance initialized the same username first.
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private String safeNickname(String nickname, String username) {
        return nickname == null || nickname.isBlank() ? username : nickname.trim();
    }
}
