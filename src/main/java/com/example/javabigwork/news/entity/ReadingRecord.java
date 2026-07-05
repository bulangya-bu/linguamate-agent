package com.example.javabigwork.news.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("reading_record")
public class ReadingRecord {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long userId;
    public Long articleId;
    public Integer readProgress;
    public Integer lookedUpWords;
    public Boolean finished;
    public LocalDateTime createdAt;
}
