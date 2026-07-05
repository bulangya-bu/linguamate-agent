package com.example.javabigwork.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.ai.dto.AiConnectionRequest;
import com.example.javabigwork.ai.dto.AiRuntimeSettings;
import com.example.javabigwork.ai.dto.AiSettingsRequest;
import com.example.javabigwork.ai.dto.AiSettingsView;
import com.example.javabigwork.ai.entity.AiSetting;
import com.example.javabigwork.ai.mapper.AiSettingMapper;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.auth.entity.UserAccount;
import com.example.javabigwork.config.AiProperties;
import java.net.URI;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiSettingsService {
    private static final Long DEFAULT_USER_ID = 1L;

    private final AiSettingMapper settingMapper;
    private final AiProperties defaults;
    private final UserContext userContext;

    public AiSettingsService(AiSettingMapper settingMapper, AiProperties defaults, UserContext userContext) {
        this.settingMapper = settingMapper;
        this.defaults = defaults;
        this.userContext = userContext;
    }

    public AiSettingsView currentView() {
        AiSetting setting = load();
        return new AiSettingsView(
                effectiveProvider(setting),
                effectiveBaseUrl(setting),
                effectiveChatModel(setting),
                setting != null && StringUtils.hasText(setting.apiKey)
        );
    }

    public AiRuntimeSettings currentRuntime() {
        AiSetting setting = load();
        return new AiRuntimeSettings(
                effectiveProvider(setting),
                effectiveBaseUrl(setting),
                effectiveApiKey(setting),
                effectiveChatModel(setting)
        );
    }

    public AiRuntimeSettings resolveConnection(AiConnectionRequest request) {
        AiRuntimeSettings current = currentRuntime();
        String baseUrl = pick(request == null ? null : request.baseUrl(), current.baseUrl());
        String apiKey = pick(request == null ? null : request.apiKey(), current.apiKey());
        return new AiRuntimeSettings(current.provider(), normalizeBaseUrl(baseUrl), apiKey, current.chatModel());
    }

    public AiSettingsView save(AiSettingsRequest request) {
        Long userId = currentUserId();
        AiSetting setting = load(userId);
        boolean existing = setting != null;
        if (setting == null) {
            setting = new AiSetting();
            setting.id = userId;
            setting.userId = userId;
        }

        String baseUrl = pick(request == null ? null : request.baseUrl(), effectiveBaseUrl(setting));
        String apiKey = pick(request == null ? null : request.apiKey(), existing ? effectiveApiKey(setting) : defaults.apiKey());
        String chatModel = pick(request == null ? null : request.chatModel(), effectiveChatModel(setting));

        setting.provider = effectiveProvider(setting);
        setting.userId = userId;
        setting.baseUrl = normalizeBaseUrl(baseUrl);
        setting.apiKey = apiKey == null ? "" : apiKey.trim();
        setting.chatModel = chatModel == null || chatModel.isBlank() ? defaults.chatModel() : chatModel.trim();
        setting.updatedAt = LocalDateTime.now();

        if (existing) {
            settingMapper.updateById(setting);
        } else {
            settingMapper.insert(setting);
        }
        return currentView();
    }

    public AiRuntimeSettings resolveCurrent() {
        return currentRuntime();
    }

    private AiSetting load() {
        return load(currentUserId());
    }

    private AiSetting load(Long userId) {
        return settingMapper.selectOne(Wrappers.<AiSetting>query().eq("user_id", userId).last("LIMIT 1"));
    }

    private Long currentUserId() {
        UserAccount user = userContext.currentUser();
        return user == null || user.id == null ? DEFAULT_USER_ID : user.id;
    }

    private String effectiveProvider(AiSetting setting) {
        return setting != null && StringUtils.hasText(setting.provider) ? setting.provider : defaults.provider();
    }

    private String effectiveBaseUrl(AiSetting setting) {
        return normalizeBaseUrl(setting != null && StringUtils.hasText(setting.baseUrl) ? setting.baseUrl : defaults.baseUrl());
    }

    private String effectiveApiKey(AiSetting setting) {
        return setting != null && StringUtils.hasText(setting.apiKey) ? setting.apiKey.trim() : defaults.apiKey();
    }

    private String effectiveChatModel(AiSetting setting) {
        return setting != null && StringUtils.hasText(setting.chatModel) ? setting.chatModel.trim() : defaults.chatModel();
    }

    private String pick(String requestValue, String fallback) {
        return StringUtils.hasText(requestValue) ? requestValue.trim() : fallback;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return defaults.baseUrl();
        }
        String trimmed = baseUrl.trim().replaceAll("/+$", "");
        if (trimmed.matches(".*?/v\\d+$")) {
            return trimmed;
        }
        try {
            URI uri = URI.create(trimmed);
            if (!StringUtils.hasText(uri.getPath()) || "/".equals(uri.getPath())) {
                return trimmed + "/v1";
            }
        } catch (Exception ignored) {
            return trimmed;
        }
        return trimmed;
    }
}
