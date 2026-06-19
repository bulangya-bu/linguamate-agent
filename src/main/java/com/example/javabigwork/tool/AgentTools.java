package com.example.javabigwork.tool;

import org.springframework.stereotype.Component;

@Component
public class AgentTools {
    private final DictionaryTool dictionaryTool;
    private final EssayScoringTool essayScoringTool;
    private final ReviewPlannerTool reviewPlannerTool;
    private final NewsRecommendTool newsRecommendTool;

    public AgentTools(
            DictionaryTool dictionaryTool,
            EssayScoringTool essayScoringTool,
            ReviewPlannerTool reviewPlannerTool,
            NewsRecommendTool newsRecommendTool
    ) {
        this.dictionaryTool = dictionaryTool;
        this.essayScoringTool = essayScoringTool;
        this.reviewPlannerTool = reviewPlannerTool;
        this.newsRecommendTool = newsRecommendTool;
    }

    public DictionaryTool dictionaryTool() {
        return dictionaryTool;
    }

    public EssayScoringTool essayScoringTool() {
        return essayScoringTool;
    }

    public ReviewPlannerTool reviewPlannerTool() {
        return reviewPlannerTool;
    }

    public NewsRecommendTool newsRecommendTool() {
        return newsRecommendTool;
    }
}
