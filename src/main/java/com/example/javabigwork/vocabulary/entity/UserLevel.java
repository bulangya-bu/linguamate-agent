package com.example.javabigwork.vocabulary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_level")
public class UserLevel {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Integer level;
    public Integer experience;
    public Integer totalExperience;
    public LocalDateTime updatedAt;
}
