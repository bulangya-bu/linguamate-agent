package com.example.javabigwork.mission.dto;

import com.example.javabigwork.agent.dto.CoachFeedback;
import com.example.javabigwork.agent.AgentTraceStep;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record MissionView(Long id, String key, LocalDate studyDate, String status, int version,
        Lesson lesson, Progress progress, String reason, LocalDateTime completedAt) {
    public record Lesson(String key, String title, String subtitle, String role, String studentRole,
            String level, String duration, String source, List<String> paragraphs, List<WordTask> words,
            List<SceneStep> steps) {}
    public record WordTask(String word, String meaning, String sentence, String translation, List<String> options) {}
    public record SceneStep(String title, String question, String purpose, List<String> hints) {}
    public record WordAnswer(int index, Long wordId, String word, String answer, boolean correct,
            String meaning, LocalDateTime createdAt) {}
    public record Coverage(String label, boolean present, List<String> evidence) {}
    public record Turn(int step, String question, String answer, int hintLevel, String reply,
            CoachFeedback feedback, List<Coverage> coverage, LocalDateTime createdAt, List<AgentTraceStep> trace) {}
    public static class Progress {
        public Lesson lesson;
        public LocalDateTime readingAt;
        public List<WordAnswer> words = new ArrayList<>();
        public List<Turn> turns = new ArrayList<>();
        public List<Integer> hints = new ArrayList<>(List.of(0, 0, 0));
        public String reflection = "";
        public LocalDateTime reflectedAt;
    }
}
