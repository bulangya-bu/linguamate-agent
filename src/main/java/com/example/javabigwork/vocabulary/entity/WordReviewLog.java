package com.example.javabigwork.vocabulary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("word_review_log")
public class WordReviewLog {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long wordId;
    public String answer;
    public Boolean correct;
    public LocalDateTime createdAt;
}
