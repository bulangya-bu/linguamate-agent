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
                StringUtils.hasText(effectiveApiKey(setting))
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
        AiSetting setting = load();
        String baseUrl = normalizeBaseUrl(pick(request == null ? null : request.baseUrl(), effectiveBaseUrl(setting)));
        String key = personalKeyFor(setting, baseUrl, request == null ? null : request.apiKey());
        return new AiRuntimeSettings(effectiveProvider(setting), baseUrl, runtimeKey(baseUrl, key),
                pick(request == null ? null : request.chatModel(), effectiveChatModel(setting)));
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

        String baseUrl = normalizeBaseUrl(pick(request == null ? null : request.baseUrl(), effectiveBaseUrl(setting)));
        String apiKey = personalKeyFor(setting, baseUrl, request == null ? null : request.apiKey());
        String chatModel = pick(request == null ? null : request.chatModel(), effectiveChatModel(setting));

        setting.provider = effectiveProvider(setting);
        setting.userId = userId;
        setting.baseUrl = baseUrl;
        setting.apiKey = apiKey;
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
        String address = setting != null && StringUtils.hasText(setting.baseUrl) ? setting.baseUrl : defaults.baseUrl();
        try {
            return normalizeBaseUrl(address);
        } catch (IllegalArgumentException ignored) {
            // A legacy invalid address can still be opened and corrected in settings.
            return "";
        }
    }

    private String effectiveApiKey(AiSetting setting) {
        return runtimeKey(effectiveBaseUrl(setting), storedPersonalKey(setting));
    }

    private String storedPersonalKey(AiSetting setting) {
        String key = setting == null ? "" : pick(setting.apiKey, "");
        // Older versions could copy the deployment credential into a personal settings row.
        return key.equals(pick(defaults.apiKey(), "")) ? "" : key;
    }

    private String runtimeKey(String destination, String personalKey) {
        if (destination.isEmpty()) return "";
        if (!personalKey.isEmpty()) return personalKey;
        return destination.equals(effectiveBaseUrl(null)) ? pick(defaults.apiKey(), "") : "";
    }

    private String personalKeyFor(AiSetting setting, String destination, String suppliedKey) {
        if (destination.isEmpty()) throw new IllegalArgumentException("请填写模型服务地址");
        boolean defaultAddress = destination.equals(effectiveBaseUrl(null));
        if (StringUtils.hasText(suppliedKey)) {
            String key = suppliedKey.trim();
            if (!key.equals(pick(defaults.apiKey(), ""))) return key;
            if (defaultAddress) return "";
            throw new IllegalArgumentException("请填写新服务对应的 API 密钥");
        }
        if (destination.equals(effectiveBaseUrl(setting))) return storedPersonalKey(setting);
        if (!defaultAddress && StringUtils.hasText(effectiveApiKey(setting))) {
            throw new IllegalArgumentException("模型地址已更改，请填写新服务对应的 API 密钥");
        }
        return "";
    }

    private String effectiveChatModel(AiSetting setting) {
        return setting != null && StringUtils.hasText(setting.chatModel) ? setting.chatModel.trim() : defaults.chatModel();
    }

    private String pick(String requestValue, String fallback) {
        return StringUtils.hasText(requestValue) ? requestValue.trim() : fallback;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) return "";
        String trimmed = baseUrl.trim().replaceAll("/+$", "");
        try {
            URI uri = URI.create(trimmed);
            if (uri.getHost() != null && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getRawUserInfo() == null && uri.getRawQuery() == null && uri.getRawFragment() == null) {
                return uri.getRawPath().isEmpty() ? trimmed + "/v1" : trimmed;
            }
        } catch (IllegalArgumentException ignored) {
            // Show the same field-level message for malformed and unsupported addresses.
        }
        throw new IllegalArgumentException("请填写有效的 HTTP 或 HTTPS 模型服务地址");
    }
}
