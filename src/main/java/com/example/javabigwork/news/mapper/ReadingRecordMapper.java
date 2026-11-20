package com.example.javabigwork.news.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javabigwork.news.entity.ReadingRecord;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface ReadingRecordMapper extends BaseMapper<ReadingRecord> {
    @Insert("""
            INSERT INTO reading_record(user_id, article_id, read_progress, looked_up_words,
                                       finished, created_at, updated_at, started_at)
            VALUES(#{userId}, #{articleId}, 0, 0, FALSE, #{now}, #{now}, #{now})
            ON DUPLICATE KEY UPDATE
              updated_at = CASE WHEN started_at IS NULL AND COALESCE(read_progress, 0) = 0
                    AND COALESCE(looked_up_words, 0) = 0 AND NOT COALESCE(finished, FALSE)
                    THEN #{now} ELSE COALESCE(updated_at, created_at, #{now}) END,
              started_at = COALESCE(started_at,
                    CASE WHEN read_progress > 0 OR looked_up_words > 0 OR finished = TRUE
                    THEN created_at END, #{now})
            """)
    void markStarted(@Param("userId") Long userId, @Param("articleId") Long articleId,
                     @Param("now") LocalDateTime now);
}
