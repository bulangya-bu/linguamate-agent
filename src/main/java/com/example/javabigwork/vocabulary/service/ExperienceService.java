package com.example.javabigwork.vocabulary.service;

import com.example.javabigwork.vocabulary.entity.UserLevel;
import com.example.javabigwork.vocabulary.mapper.UserLevelMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExperienceService {
    private final UserLevelMapper levelMapper;

    public ExperienceService(UserLevelMapper levelMapper) {
        this.levelMapper = levelMapper;
    }

    private UserLevel getLevel() {
        UserLevel level = levelMapper.selectById(1);
        if (level == null) {
            level = new UserLevel();
            level.id = 1L;
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
        level.experience += amount;
        level.totalExperience += amount;
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
        return new LevelResult(level.level, level.experience, level.totalExperience, xpForNextLevel(level.level), false);
    }

    public record LevelResult(int level, int experience, int totalExperience, int nextLevelXp, boolean levelUp) {
    }
}
