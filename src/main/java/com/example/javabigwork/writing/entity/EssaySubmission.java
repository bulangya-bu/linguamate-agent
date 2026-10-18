package com.example.javabigwork.writing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("essay_submission")
public class EssaySubmission {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long userId;
    public String title;
    public String examType;
    public String originalText;
    public String polishedText;
    public Double totalScore;
    public Long revisionOfId;
    public String reviewSource;
    public String modelName;
    public String targetLevel;
    public LocalDateTime createdAt;
}
