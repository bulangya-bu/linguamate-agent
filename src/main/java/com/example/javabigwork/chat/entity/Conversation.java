package com.example.javabigwork.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("conversation")
public class Conversation {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String scene;
    public String title;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
