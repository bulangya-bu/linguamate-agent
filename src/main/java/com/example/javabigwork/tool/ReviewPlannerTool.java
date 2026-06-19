package com.example.javabigwork.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.vocabulary.entity.UserWordProgress;
import com.example.javabigwork.vocabulary.mapper.UserWordProgressMapper;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReviewPlannerTool {
    private static final Logger log = LoggerFactory.getLogger(ReviewPlannerTool.class);
    private final UserWordProgressMapper progressMapper;

    public ReviewPlannerTool(UserWordProgressMapper progressMapper) {
        this.progressMapper = progressMapper;
    }

    public List<UserWordProgress> planToday(int limit) {
        log.info("ReviewPlannerTool.planToday: limit={}", limit);
        LocalDateTime now = LocalDateTime.now();
        return progressMapper.selectList(Wrappers.<UserWordProgress>query()
                        .le("next_review_at", now)
                        .or()
                        .gt("wrong_count", 0))
                .stream()
                .sorted(Comparator
                        .comparing((UserWordProgress progress) -> progress.wrongCount == null ? 0 : -progress.wrongCount)
                        .thenComparing(progress -> progress.nextReviewAt == null ? now : progress.nextReviewAt))
                .limit(Math.max(1, limit))
                .toList();
    }
}
