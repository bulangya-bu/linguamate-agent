package com.example.javabigwork.dictionary.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class OfflineDictionaryServiceTests {
    private final OfflineDictionaryService dictionary = new OfflineDictionaryService(new ObjectMapper());

    @Test void commonReadingWordsHaveRealChineseDefinitionsAndNoInventedExamples() {
        for (String input : List.of("language", "scientist", "researcher", "the", "an", "method", "uncertain", "connection", "responsible")) {
            var word = dictionary.lookup(input).orElseThrow(() -> new AssertionError("Missing " + input));
            assertTrue(word.meaningCn.matches("(?s).*[\\p{IsHan}].*"), input);
            assertTrue(word.meaningCn.length() <= 255, input);
            assertFalse(word.meaningCn.contains("未收录"), input);
            assertTrue(word.exampleEn.isEmpty(), input);
        }
        assertTrue(dictionary.lookup("language").orElseThrow().meaningCn.contains("语言"));
        assertTrue(dictionary.lookup("language").orElseThrow().phoneticUs.isEmpty());
        assertFalse(dictionary.lookup("language").orElseThrow().phoneticUk.isEmpty());
    }

    @Test void inflectionsAndContractionsAreSupportedWithoutDestroyingIndependentSenses() {
        assertEquals("go", dictionary.lookup("went").orElseThrow().word);
        assertEquals("scientist", dictionary.lookup("scientists").orElseThrow().word);
        assertEquals("researcher", dictionary.lookup("researchers").orElseThrow().word);
        assertEquals("child", dictionary.lookup("children").orElseThrow().word);
        assertEquals("running", dictionary.lookup("running").orElseThrow().word);
        assertEquals("better", dictionary.lookup("better").orElseThrow().word);
        assertTrue(dictionary.lookup("can't").orElseThrow().meaningCn.contains("不能"));
        assertTrue(dictionary.lookup("wouldn't").orElseThrow().meaningCn.contains("不会"));
        assertTrue(dictionary.lookup("haven't").orElseThrow().meaningCn.contains("没有"));
        assertTrue(dictionary.lookup("notarealwordxyzxyz").isEmpty());
    }

    @Test void callersReceiveIndependentCopiesRatherThanMutatingTheSharedIndex() {
        var first = dictionary.lookup("language").orElseThrow();
        first.id = 99L; first.meaningCn = "changed";
        var second = dictionary.lookup("language").orElseThrow();
        assertNull(second.id);
        assertTrue(second.meaningCn.contains("语言"));
    }
}
