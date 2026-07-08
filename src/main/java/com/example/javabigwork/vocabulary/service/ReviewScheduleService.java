package com.example.javabigwork.vocabulary.service;

import com.example.javabigwork.tool.ReviewPlannerTool;
import com.example.javabigwork.vocabulary.dto.WordCardView;
import com.example.javabigwork.vocabulary.entity.Word;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReviewScheduleService {
    private final ReviewPlannerTool reviewPlannerTool;
    private final VocabularyService vocabularyService;

    public ReviewScheduleService(ReviewPlannerTool reviewPlannerTool, VocabularyService vocabularyService) {
        this.reviewPlannerTool = reviewPlannerTool;
        this.vocabularyService = vocabularyService;
    }

    public List<WordCardView> todayReviewWords(int limit) {
        List<WordCardView> planned = reviewPlannerTool.planToday(limit)
                .stream()
                .map(progress -> {
                    Word word = vocabularyService.getWord(progress.wordId);
                    return word == null ? null : vocabularyService.toView(word);
                })
                .filter(item -> item != null)
                .toList();
        return planned;
    }

    public List<WordCardView> favoriteWords(int limit) {
        return vocabularyService.favoriteWords(limit);
    }

    public List<WordCardView> masteredWords(int limit) {
        return vocabularyService.masteredWords(limit);
    }
}
