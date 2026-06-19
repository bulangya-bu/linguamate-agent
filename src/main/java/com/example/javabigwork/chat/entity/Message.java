package com.example.javabigwork.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long conversationId;
    public String role;
    public String content;
    public String metadata;
    public LocalDateTime createdAt;
}
