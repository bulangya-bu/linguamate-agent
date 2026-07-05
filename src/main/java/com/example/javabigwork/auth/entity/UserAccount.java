package com.example.javabigwork.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_account")
public class UserAccount {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String username;
    public String passwordHash;
    public String nickname;
    public String role;
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
