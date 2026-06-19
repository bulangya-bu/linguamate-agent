package com.example.javabigwork.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Bean
    public RagRuntimeOptions ragRuntimeOptions() {
        return new RagRuntimeOptions(3, 260);
    }

    public record RagRuntimeOptions(int maxSnippets, int maxPromptSnippetChars) {
    }
}
