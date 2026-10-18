package com.example.javabigwork.vocabulary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_word_progress")
public class UserWordProgress {
    public String sourceSentence;
    public String sourceTitle;
    public String sourceWord;
    public Long sourceArticleId;
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long userId;
    public Long wordId;
    public Integer familiarity;
    public Integer wrongCount;
    public Integer reviewCount;
    public Boolean favorite;
    public LocalDateTime nextReviewAt;
    public LocalDateTime lastReviewAt;
}
