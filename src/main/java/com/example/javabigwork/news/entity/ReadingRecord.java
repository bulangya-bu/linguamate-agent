package com.example.javabigwork.news.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("reading_record")
public class ReadingRecord {
    // Legacy detail requests also created zero-progress rows. Only user activity counts.
    public static final String HAS_ACTIVITY =
            "(started_at IS NOT NULL OR read_progress > 0 OR looked_up_words > 0 OR finished = TRUE)";
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long userId;
    public Long articleId;
    public Integer readProgress;
    public Integer lookedUpWords;
    public Boolean finished;
    public LocalDateTime startedAt;
    public LocalDateTime finishedAt;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
