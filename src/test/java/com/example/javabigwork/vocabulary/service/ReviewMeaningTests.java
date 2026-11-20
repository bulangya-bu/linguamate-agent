package com.example.javabigwork.vocabulary.service;

import com.example.javabigwork.vocabulary.dto.WordDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReviewMeaningTests {
    @Test void aNegatedOrPartiallyOverlappingAnswerCannotPass() {
        var legal = List.of(new WordDefinition("adj", "法律的； 合法的"));
        for (String wrong : List.of("不合法的", "这是不合法的", "法", "非法，合法的", "；")) {
            assertFalse(VocabularyService.acceptsReviewMeaning(legal, wrong), wrong);
        }
        assertFalse(VocabularyService.acceptsReviewMeaning(List.of(new WordDefinition("adj", "不合法的")), "合法"));
        for (String right : List.of("合法的", "合法", "法律的；合法的。")) {
            assertTrue(VocabularyService.acceptsReviewMeaning(legal, right), right);
        }
    }

    @Test void independentOneCharacterAndAlternativeDictionarySensesCount() {
        var scratch = List.of(new WordDefinition("v", "搔，抓；刮擦"), new WordDefinition("n", "抓痕"));
        for (String right : List.of("抓", "搔", "刮擦", "抓痕", "搔、抓")) {
            assertTrue(VocabularyService.acceptsReviewMeaning(scratch, right), right);
        }
        assertFalse(VocabularyService.acceptsReviewMeaning(scratch, "痕"));
        var objective = List.of(new WordDefinition("n", "[光学]物镜；目标（尤指长期目标）"));
        assertTrue(VocabularyService.acceptsReviewMeaning(objective, "物镜"));
        assertTrue(VocabularyService.acceptsReviewMeaning(objective, "目标"));
        assertFalse(VocabularyService.acceptsReviewMeaning(objective, "光学"));
    }
}
