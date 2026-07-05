package com.example.javabigwork.vocabulary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("word_ai_detail_cache")
public class WordAiDetailCache {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long wordId;
    public String modelName;
    public String contentJson;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
