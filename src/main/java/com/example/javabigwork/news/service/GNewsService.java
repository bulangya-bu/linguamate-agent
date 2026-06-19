package com.example.javabigwork.news.service;

import com.example.javabigwork.ai.dto.AiRuntimeSettings;
import com.example.javabigwork.ai.dto.OpenAiChatMessage;
import com.example.javabigwork.ai.service.AiSettingsService;
import com.example.javabigwork.ai.service.OpenAiCompatibleClient;
import com.example.javabigwork.news.dto.GNewsArticleView;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class GNewsService {
    private static final String BASE_URL = "https://gnews.io/api/v4/search";

    private final AiSettingsService settingsService;
    private final OpenAiCompatibleClient aiClient;
    private final RestClient restClient;
    private final String apiKey;

    public GNewsService(
            AiSettingsService settingsService,
            OpenAiCompatibleClient aiClient,
            @Value("${linguamate.news.gnews-api-key:}") String apiKey
    ) {
        this.settingsService = settingsService;
        this.aiClient = aiClient;
        this.restClient = RestClient.create();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public List<GNewsArticleView> search(String query, int max) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(query)) {
            return List.of();
        }
        int safeMax = Math.max(1, Math.min(10, max));
        String url = BASE_URL + "?q=" + query + "&lang=en&max=" + safeMax + "&apikey=" + apiKey;
        GNewsResponse response;
        try {
            response = restClient.get().uri(url).retrieve().body(GNewsResponse.class);
        } catch (Exception e) {
            return List.of();
        }
        if (response == null || response.articles() == null) {
            return List.of();
        }
        return response.articles().stream()
                .limit(safeMax)
                .map(this::toView)
                .toList();
    }

    private GNewsArticleView toView(GNewsArticle article) {
        String title = article.title() != null ? article.title() : "";
        String description = article.description() != null ? article.description() : "";
        String titleCn = translate(title);
        String descriptionCn = translate(description);
        return new GNewsArticleView(
                title,
                titleCn,
                description,
                descriptionCn,
                article.content() != null ? article.content() : "",
                article.url() != null ? article.url() : "",
                article.source() != null ? article.source().name() : "",
                article.publishedAt() != null ? article.publishedAt() : ""
        );
    }

    public String translate(String text) {
        if (!StringUtils.hasText(text)) return "";
        AiRuntimeSettings settings = settingsService.resolveCurrent();
        if (!StringUtils.hasText(settings.baseUrl()) || !StringUtils.hasText(settings.apiKey())) {
            return "";
        }
        try {
            String prompt = "Translate the following English text to Simplified Chinese. Return ONLY the Chinese translation, nothing else:\n\n" + text;
            List<OpenAiChatMessage> messages = new ArrayList<>();
            messages.add(new OpenAiChatMessage("user", prompt));
            String result = aiClient.chat(settings, "You are a translator. Translate English to Chinese concisely.", messages);
            return result != null ? result.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private record GNewsResponse(@JsonProperty("totalArticles") int totalArticles,
                                  @JsonProperty("articles") List<GNewsArticle> articles) {
    }

    private record GNewsArticle(@JsonProperty("title") String title,
                                 @JsonProperty("description") String description,
                                 @JsonProperty("content") String content,
                                 @JsonProperty("url") String url,
                                 @JsonProperty("image") String image,
                                 @JsonProperty("publishedAt") String publishedAt,
                                 @JsonProperty("source") GNewsSource source) {
    }

    private record GNewsSource(@JsonProperty("name") String name) {
    }
}
