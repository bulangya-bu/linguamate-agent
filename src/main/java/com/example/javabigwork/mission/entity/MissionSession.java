package com.example.javabigwork.mission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("learning_mission_session")
public class MissionSession {
    @TableId(type = IdType.AUTO) public Long id;
    public Long userId;
    public String missionKey;
    public LocalDate studyDate;
    public String contentJson;
    public String status;
    public Integer version;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDateTime completedAt;
}
