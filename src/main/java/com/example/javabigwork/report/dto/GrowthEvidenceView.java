package com.example.javabigwork.report.dto;

import com.example.javabigwork.mission.dto.MissionView;
import java.time.LocalDateTime;
import java.util.List;

public record GrowthEvidenceView(List<WordEvidence> words,List<WritingEvidence> writing,List<MissionView> missions,List<ExpressionEvidence> expressions) {
    public record ExpressionEvidence(Long id,Long conversationId,String title,String answer,String reply,
            com.example.javabigwork.agent.dto.CoachFeedback feedback,LocalDateTime createdAt) {}
    public record Attempt(Long id,String answer,boolean correct,String type,LocalDateTime createdAt) {}
    public record WordEvidence(Long wordId,String word,String meaning,List<Attempt> attempts) {}
    public record WritingEvidence(Long id,String title,Long previousId,String before,String after,String suggestion,
            Double previousScore,Double score,boolean comparable,String source,String model,LocalDateTime createdAt) {}
}
