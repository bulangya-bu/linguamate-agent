package com.example.javabigwork.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("knowledge_chunk")
public class KnowledgeChunk {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long documentId;
    public Integer chunkIndex;
    public String content;
    public String keywords;
    public String embeddingRef;
    public LocalDateTime createdAt;
}
