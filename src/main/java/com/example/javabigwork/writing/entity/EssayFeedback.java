package com.example.javabigwork.writing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("essay_feedback")
public class EssayFeedback {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long essayId;
    public String category;
    public String originalSentence;
    public String suggestion;
    public String correctedSentence;
}
