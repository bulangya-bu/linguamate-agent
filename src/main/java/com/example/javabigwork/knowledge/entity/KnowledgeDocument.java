package com.example.javabigwork.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("knowledge_document")
public class KnowledgeDocument {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String title;
    public String sourceType;
    public String tags;
    public String content;
    public LocalDateTime createdAt;
}
