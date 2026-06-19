package com.example.javabigwork.news.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("news_article")
public class NewsArticle {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String title;
    public String summary;
    public String content;
    public String sourceName;
    public String difficulty;
    public String topicTags;
    public LocalDateTime createdAt;
}
