package com.example.javabigwork.vocabulary.service;

import com.example.javabigwork.vocabulary.entity.Word;

public final class WordExampleFallback {
    private WordExampleFallback() {
    }

    public static String english(Word word) {
        if (word != null && hasText(word.exampleEn)) {
            return word.exampleEn.trim();
        }
        return englishFor(word == null ? null : word.word);
    }

    public static String chinese(Word word) {
        if (word != null && hasText(word.exampleCn)) {
            return word.exampleCn.trim();
        }
        return chineseFor(word == null ? null : word.word);
    }

    public static String englishFor(String word) {
        return "The word \"" + displayWord(word, "this word") + "\" is useful in English practice.";
    }

    public static String chineseFor(String word) {
        return "单词“" + displayWord(word, "这个单词") + "”在英语练习中很有用。";
    }

    private static String displayWord(String word, String fallback) {
        return hasText(word) ? word.trim() : fallback;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
