package com.example.javabigwork.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.javabigwork.auth.dto.RegisterRequest;
import com.example.javabigwork.auth.entity.UserAccount;
import com.example.javabigwork.auth.mapper.UserAccountMapper;
import com.example.javabigwork.cache.RedisCacheService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {
    private static final String LEGACY_ADMIN_PASSWORD = "admin123";
    private static final String TEST_ADMIN_PASSWORD = "test-admin-bootstrap";

    @Mock private UserAccountMapper userMapper;
    @Mock private RedisCacheService redisCache;
    @Mock private LoginActivityService loginActivity;
    @Mock private HttpServletRequest request;
    @Captor private ArgumentCaptor<UserAccount> insertedUser;
    @Captor private ArgumentCaptor<Wrapper<UserAccount>> update;

    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void startupWithoutConfigurationOnlyCreatesTheDemoLearner() {
        service("").run(null);

        verify(userMapper).insert(insertedUser.capture());
        assertEquals("student", insertedUser.getValue().username);
        assertEquals("USER", insertedUser.getValue().role);
        verify(userMapper, never()).update(isNull(), any());
    }

    @Test
    void startupCreatesAdminWithExplicitPassword() {
        service(TEST_ADMIN_PASSWORD).run(null);

        verify(userMapper, times(2)).insert(insertedUser.capture());
        UserAccount admin = insertedUser.getAllValues().get(0);
        assertEquals("admin", admin.username);
        assertEquals("ADMIN", admin.role);
        assertTrue(passwordHasher.matches(TEST_ADMIN_PASSWORD, admin.passwordHash));
    }

    @Test
    void startupRejectsTheLegacyPasswordForNewAdmin() {
        assertThrows(IllegalStateException.class, () -> service(LEGACY_ADMIN_PASSWORD).run(null));
        verify(userMapper, never()).insert(any(UserAccount.class));
    }

    @Test
    void startupUpdatesThePasswordOnlyWhenItStillMatchesTheLegacyDefault() {
        UserAccount admin = adminWithPassword(LEGACY_ADMIN_PASSWORD);
        when(userMapper.selectList(any())).thenReturn(List.of(admin), List.of());

        service(TEST_ADMIN_PASSWORD).run(null);

        verify(userMapper).update(isNull(), update.capture());
        UpdateWrapper<UserAccount> passwordUpdate = (UpdateWrapper<UserAccount>) update.getValue();
        assertEquals(List.of("password_hash", "updated_at"),
                Arrays.stream(passwordUpdate.getSqlSet().split(","))
                        .map(assignment -> assignment.substring(0, assignment.indexOf('=')))
                        .toList());
        assertTrue(passwordUpdate.getSqlSegment().contains("password_hash ="));
        assertTrue(passwordUpdate.getParamNameValuePairs().containsValue(admin.passwordHash));
        assertTrue(passwordUpdate.getParamNameValuePairs().values().stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .anyMatch(hash -> passwordHasher.matches(TEST_ADMIN_PASSWORD, hash)));
    }

    @Test
    void startupDoesNotReplaceAnExistingChangedPasswordOrAccountStatus() {
        UserAccount admin = adminWithPassword("previous-test-admin-password");
        when(userMapper.selectList(any())).thenReturn(List.of(admin), List.of());

        service(TEST_ADMIN_PASSWORD).run(null);

        verify(userMapper, never()).update(isNull(), any());
        verify(userMapper, never()).updateById(any(UserAccount.class));
        verify(userMapper).insert(insertedUser.capture());
        assertEquals("student", insertedUser.getValue().username);
    }

    @Test
    void duplicateDetectedDuringInsertDoesNotStartASession() {
        when(userMapper.insert(any(UserAccount.class))).thenThrow(DuplicateKeyException.class);

        assertThrows(UsernameAlreadyExistsException.class,
                () -> service("").register(new RegisterRequest("learner", "password123", null), request));
        verifyNoInteractions(request);
    }

    private AuthService service(String bootstrapPassword) {
        return new AuthService(userMapper, passwordHasher, redisCache, loginActivity, bootstrapPassword);
    }

    private UserAccount adminWithPassword(String password) {
        UserAccount admin = new UserAccount();
        admin.id = 42L;
        admin.username = "admin";
        admin.role = "ADMIN";
        admin.status = "DISABLED";
        admin.passwordHash = passwordHasher.hash(password);
        return admin;
    }
}
