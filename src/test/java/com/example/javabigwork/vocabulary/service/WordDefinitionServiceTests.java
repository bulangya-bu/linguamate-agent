package com.example.javabigwork.vocabulary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.javabigwork.vocabulary.dto.WordDefinition;
import com.example.javabigwork.vocabulary.entity.Word;
import java.util.List;
import org.junit.jupiter.api.Test;

class WordDefinitionServiceTests {
    @Test
    void mergesAllBanksWithoutDroppingPartsOfSpeechOrChangingThePrimaryAnswer() {
        WordDefinitionService service = new WordDefinitionService();
        service.register("Revolutionary", List.of(
                new WordDefinition("adj", "革命的，革新的"),
                new WordDefinition("n", "革命者")));
        service.register("revolutionary", List.of(
                new WordDefinition("adj.", "革命的, 革新的"),
                new WordDefinition("n", "革命支持者")));
        Word word = new Word();
        word.word = "revolutionary";
        word.partOfSpeech = "adj";
        word.meaningCn = "革命的，革新的";

        assertEquals(List.of(
                new WordDefinition("adj", "革命的，革新的"),
                new WordDefinition("n", "革命者"),
                new WordDefinition("n", "革命支持者")), service.definitionsFor(word));
        assertEquals("革命的，革新的", word.meaningCn);
    }

    @Test
    void wordsOutsideTheBundledBanksKeepTheirExistingDefinition() {
        Word word = new Word();
        word.word = "custom-word";
        word.partOfSpeech = "n.";
        word.meaningCn = "自定义词条";
        assertEquals(List.of(new WordDefinition("n.", "自定义词条")),
                new WordDefinitionService().definitionsFor(word));
    }
}
