package com.example.javabigwork.vocabulary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("word_review_log")
public class WordReviewLog {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long userId;
    public Long wordId;
    public String answer;
    public String spellingAnswer;
    public String meaningAnswer;
    public Boolean correct;
    public Boolean spellingCorrect;
    public Boolean meaningCorrect;
    public String reviewType;
    public LocalDateTime createdAt;
}
