package com.example.javabigwork.ai.service;

import com.example.javabigwork.ai.dto.AiConnectionRequest;
import com.example.javabigwork.ai.dto.AiSettingsRequest;
import com.example.javabigwork.ai.entity.AiSetting;
import com.example.javabigwork.ai.mapper.AiSettingMapper;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiSettingsServiceTests {
    private static final String DEFAULT_ADDRESS = "https://primary.example/v1";
    private static final String OTHER = "https://custom.example/v1";
    private final AiSettingMapper mapper = mock(AiSettingMapper.class);
    private final AiSettingsService service = new AiSettingsService(mapper,
            new AiProperties("fixture", DEFAULT_ADDRESS, "deployment-fixture", "model", null), mock(UserContext.class));
    private AiSetting row;

    @BeforeEach
    void loadRow() {
        when(mapper.selectOne(any())).thenAnswer(invocation -> row);
    }

    @Test
    void defaultCredentialIsAvailableOnlyAtItsOwnAddressAndIsNeverSavedInPersonalSettings() {
        assertTrue(service.currentView().hasApiKey());
        assertEquals("deployment-fixture", service.resolveConnection(new AiConnectionRequest("https://primary.example/", "")).apiKey());
        assertThrows(IllegalArgumentException.class, () -> service.resolveConnection(new AiConnectionRequest(OTHER, "")));
        assertThrows(IllegalArgumentException.class, () -> service.save(new AiSettingsRequest(OTHER, "", "model")));
        service.save(new AiSettingsRequest(DEFAULT_ADDRESS, "", "other-model"));
        ArgumentCaptor<AiSetting> inserted = ArgumentCaptor.forClass(AiSetting.class);
        verify(mapper).insert(inserted.capture());
        assertEquals("", inserted.getValue().apiKey);
    }

    @Test
    void credentialsRemainScopedAcrossUnsavedTestsSavesAndModelChanges() {
        row = new AiSetting();
        row.baseUrl = OTHER;
        row.apiKey = "personal-fixture";
        assertEquals("personal-fixture", service.resolveConnection(new AiConnectionRequest(OTHER + "/", "")).apiKey());
        String next = "https://custom.example/second/v1";
        assertThrows(IllegalArgumentException.class, () -> service.resolveConnection(new AiConnectionRequest(next, "")));
        assertThrows(IllegalArgumentException.class, () -> service.save(new AiSettingsRequest(next, "", "model")));
        assertEquals("second-fixture", service.resolveConnection(new AiConnectionRequest(next, "second-fixture")).apiKey());
        assertEquals(OTHER, row.baseUrl);
        service.save(new AiSettingsRequest(next, "second-fixture", "model"));
        service.save(new AiSettingsRequest(next, "", "other-model"));
        assertEquals("second-fixture", service.currentRuntime().apiKey());
        service.save(new AiSettingsRequest(DEFAULT_ADDRESS, "", "model"));
        assertEquals("", row.apiKey);
        assertEquals("deployment-fixture", service.currentRuntime().apiKey());
    }

    @Test
    void historicalDefaultKeyCopiesAndEmptyCustomKeysDoNotReachCustomServices() {
        row = new AiSetting();
        row.baseUrl = OTHER;
        row.apiKey = "deployment-fixture";
        assertFalse(service.currentView().hasApiKey());
        assertEquals("", service.currentRuntime().apiKey());
        assertThrows(IllegalArgumentException.class, () -> service.save(new AiSettingsRequest(OTHER, "deployment-fixture", "model")));
        service.save(new AiSettingsRequest(OTHER, "", "model"));
        assertEquals("", row.apiKey);
        assertEquals("", service.resolveConnection(null).apiKey());
    }
}
