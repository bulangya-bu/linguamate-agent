package com.example.javabigwork.vocabulary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.vocabulary.entity.UserLevel;
import com.example.javabigwork.vocabulary.mapper.UserLevelMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExperienceService {
    private final UserLevelMapper levelMapper;
    private final UserContext userContext;

    public ExperienceService(UserLevelMapper levelMapper, UserContext userContext) {
        this.levelMapper = levelMapper;
        this.userContext = userContext;
    }

    private UserLevel getLevel() {
        Long userId = userContext.userId();
        UserLevel level = levelMapper.selectList(Wrappers.<UserLevel>query()
                        .eq("user_id", userId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
        if (level == null) {
            level = new UserLevel();
            level.userId = userId;
            level.level = 1;
            level.experience = 0;
            level.totalExperience = 0;
            level.updatedAt = LocalDateTime.now();
            levelMapper.insert(level);
        }
        return level;
    }

    public int xpForNextLevel(int currentLevel) {
        return currentLevel * 100;
    }

    @Transactional
    public LevelResult addXp(int amount) {
        UserLevel level = getLevel();
        level.level = level.level == null ? 1 : level.level;
        level.experience = value(level.experience) + amount;
        level.totalExperience = value(level.totalExperience) + amount;
        int nextLevelXp = xpForNextLevel(level.level);
        boolean levelUp = false;
        while (level.experience >= nextLevelXp) {
            level.experience -= nextLevelXp;
            level.level++;
            nextLevelXp = xpForNextLevel(level.level);
            levelUp = true;
        }
        level.updatedAt = LocalDateTime.now();
        levelMapper.updateById(level);
        return new LevelResult(level.level, level.experience, level.totalExperience, nextLevelXp, levelUp);
    }

    public LevelResult getStatus() {
        UserLevel level = getLevel();
        level.level = level.level == null ? 1 : level.level;
        return new LevelResult(level.level, level.experience, level.totalExperience, xpForNextLevel(level.level), false);
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    public record LevelResult(int level, int experience, int totalExperience, int nextLevelXp, boolean levelUp) {
    }
}
