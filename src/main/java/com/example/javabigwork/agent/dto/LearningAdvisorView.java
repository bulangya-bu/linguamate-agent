package com.example.javabigwork.agent.dto;

import java.util.List;

public record LearningAdvisorView(
        String reply,
        LearningCard learningCard,
        List<ChatAction> actions) {
}
