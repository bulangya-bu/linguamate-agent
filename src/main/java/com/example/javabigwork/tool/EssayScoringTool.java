package com.example.javabigwork.tool;

import com.example.javabigwork.writing.dto.EssayReviewRequest;
import com.example.javabigwork.writing.dto.SentenceFeedbackView;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EssayScoringTool {
    private static final Logger log = LoggerFactory.getLogger(EssayScoringTool.class);

    public EssayScoringResult score(EssayReviewRequest request) {
        log.info("EssayScoringTool.score: wordCount={}", request.essayText().trim().split("\\s+").length);
        String essay = request.essayText().trim();
        int words = essay.isBlank() ? 0 : essay.split("\\s+").length;
        double lengthScore = Math.min(25, words / 8.0);
        double grammarScore = essay.matches(".*\\b(is|are|was|were|have|has|had)\\b.*") ? 22 : 17;
        double vocabularyScore = essay.matches(".*\\b(however|therefore|moreover|significant|essential)\\b.*") ? 23 : 18;
        double structureScore = essay.contains(".") && essay.length() > 120 ? 22 : 17;
        double logicScore = essay.matches("(?is).*(first|second|finally|in conclusion|because|so).*") ? 22 : 18;
        double total = Math.min(100, lengthScore + grammarScore + vocabularyScore + structureScore + logicScore);

        List<SentenceFeedbackView> feedback = buildFeedback(essay);
        String polished = polish(essay);
        String summary = "作文已完成初步批改：重点关注语法准确性、连接词使用和段落结构。";
        return new EssayScoringResult(total, grammarScore, vocabularyScore, structureScore, logicScore, summary, polished, feedback);
    }

    private List<SentenceFeedbackView> buildFeedback(String essay) {
        List<SentenceFeedbackView> feedback = new ArrayList<>();
        if (essay.toLowerCase().contains("i think")) {
            feedback.add(new SentenceFeedbackView(
                    "vocabulary",
                    "I think ...",
                    "学术写作中可减少重复使用 I think。",
                    "It can be argued that ..."
            ));
        }
        if (!essay.matches("(?is).*(however|therefore|moreover|in addition|in conclusion).*")) {
            feedback.add(new SentenceFeedbackView(
                    "structure",
                    "全文",
                    "建议增加连接词，让论证层次更清楚。",
                    "However / Therefore / In addition / In conclusion"
            ));
        }
        if (essay.split("\\s+").length < 80) {
            feedback.add(new SentenceFeedbackView(
                    "logic",
                    "全文",
                    "内容偏短，建议增加例子或原因解释。",
                    "Add one concrete example to support your main idea."
            ));
        }
        if (feedback.isEmpty()) {
            feedback.add(new SentenceFeedbackView(
                    "grammar",
                    "全文",
                    "整体表达清楚，可以继续提升词汇多样性。",
                    "Use more varied sentence patterns where appropriate."
            ));
        }
        return feedback;
    }

    private String polish(String essay) {
        return essay
                .replaceAll("(?i)\\bi think\\b", "It can be argued that")
                .replaceAll("(?i)\\bvery important\\b", "essential")
                .replaceAll("(?i)\\bgood\\b", "beneficial");
    }
}
