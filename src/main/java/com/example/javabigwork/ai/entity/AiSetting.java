package com.example.javabigwork.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ai_setting")
public class AiSetting {
    @TableId(type = IdType.INPUT)
    public Long id;
    public String provider;
    public String baseUrl;
    public String apiKey;
    public String chatModel;
    public LocalDateTime updatedAt;
}
