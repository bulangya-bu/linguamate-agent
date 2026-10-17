package com.example.javabigwork.agent.dto;

import java.util.List;

/** Feedback is generated together with the reply; word usage is checked by the server. */
public record CoachFeedback(
        String source, String modelName, String notice, String original,
        String revisionType, String improved, String explanation, String nextChallenge,
        List<Phrase> phrases, List<TargetWord> targetWords) {
    public record Phrase(String text, String meaningCn) {}
    public record TargetWord(Long id, String word, String meaningCn, boolean used) {}
}
