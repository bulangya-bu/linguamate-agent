package com.example.javabigwork.vocabulary.service;

import com.example.javabigwork.vocabulary.entity.Word;

public final class WordExampleFallback {
    private static final java.util.Map<String,String[]> CURATED = java.util.Map.ofEntries(
        example("abide", "All visitors must abide by the library rules.", "所有访客都必须遵守图书馆的规定。"),
        example("accept", "Jamie accepted our invitation to visit the campus.", "杰米接受了我们参观校园的邀请。"),
        example("reduce", "Turning off the tap helps reduce water waste.", "关掉水龙头有助于减少水资源浪费。"),
        example("sensor", "A sensor detects a hand near the tap.", "传感器检测水龙头附近的手。"),
        example("waste", "A leaking tap can waste a lot of water.", "漏水的水龙头会浪费大量的水。"),
        example("library", "The library is beside the student centre.", "图书馆在学生中心旁边。"),
        example("direction", "She pointed in the direction of the main gate.", "她指向大门的方向。"),
        example("available", "Quiet study spaces are available until six.", "安静的学习空间可以使用到六点。"),
        example("report", "Please report a leaking tap to the campus team.", "请向校园维护团队报告漏水的水龙头。"),
        example("improve", "Clear examples can improve your explanation.", "清楚的例子能改进你的解释。"),
        example("compare", "We will compare water use before and after the test.", "我们将比较测试前后的用水量。"),
        example("evidence", "We need more evidence before drawing a conclusion.", "我们需要更多证据才能得出结论。")
    );
    private static java.util.Map.Entry<String,String[]> example(String word,String en,String cn) {
        return java.util.Map.entry(word,new String[]{en,cn});
    }
    private static String[] curated(String word) { return word==null?null:CURATED.get(word.trim().toLowerCase(java.util.Locale.ROOT)); }
    private static boolean placeholder(String value) {
        return value!=null && (value.startsWith("The word \"") && value.endsWith("is useful in English practice.") || value.startsWith("单词“") && value.endsWith("在英语练习中很有用。"));
    }
    private WordExampleFallback() {
    }

    public static String english(Word word) {
        if (word != null && hasText(word.exampleEn) && !(placeholder(word.exampleEn) && curated(word.word)!=null)) {
            return word.exampleEn.trim();
        }
        return englishFor(word == null ? null : word.word);
    }

    public static String chinese(Word word) {
        if (word != null && hasText(word.exampleCn) && !(placeholder(word.exampleCn) && curated(word.word)!=null)) {
            return word.exampleCn.trim();
        }
        return chineseFor(word == null ? null : word.word);
    }

    public static String englishFor(String word) {
        if(curated(word)!=null)return curated(word)[0];
        return "The word \"" + displayWord(word, "this word") + "\" is useful in English practice.";
    }

    public static String chineseFor(String word) {
        if(curated(word)!=null)return curated(word)[1];
        return "单词“" + displayWord(word, "这个单词") + "”在英语练习中很有用。";
    }

    private static String displayWord(String word, String fallback) {
        return hasText(word) ? word.trim() : fallback;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
