package com.example.javabigwork.report.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.auth.UserContext;
import com.example.javabigwork.news.entity.ReadingRecord;
import com.example.javabigwork.news.mapper.ReadingRecordMapper;
import com.example.javabigwork.report.dto.LearningReportView;
import com.example.javabigwork.vocabulary.entity.UserWordProgress;
import com.example.javabigwork.vocabulary.mapper.UserWordProgressMapper;
import com.example.javabigwork.writing.entity.EssaySubmission;
import com.example.javabigwork.writing.mapper.EssaySubmissionMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final UserWordProgressMapper progressMapper;
    private final EssaySubmissionMapper essayMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final UserContext userContext;

    public ReportService(
            UserWordProgressMapper progressMapper,
            EssaySubmissionMapper essayMapper,
            ReadingRecordMapper readingRecordMapper,
            UserContext userContext
    ) {
        this.progressMapper = progressMapper;
        this.essayMapper = essayMapper;
        this.readingRecordMapper = readingRecordMapper;
        this.userContext = userContext;
    }

    public LearningReportView summary() {
        Long userId = userContext.userId();
        long learnedWords = progressMapper.selectCount(Wrappers.<UserWordProgress>query()
                .eq("user_id", userId)
                .ge("familiarity", 3));
        long reviewWords = progressMapper.selectCount(Wrappers.<UserWordProgress>query()
                .eq("user_id", userId)
                .gt("wrong_count", 0));
        long dueWords = progressMapper.selectCount(Wrappers.<UserWordProgress>query()
                .eq("user_id", userId)
                .le("next_review_at", LocalDateTime.now()));
        long favoriteWords = progressMapper.selectCount(Wrappers.<UserWordProgress>query()
                .eq("user_id", userId)
                .eq("favorite", true));
        long essayCount = essayMapper.selectCount(Wrappers.<EssaySubmission>query().eq("user_id", userId));
        List<ReadingRecord> readings = readingRecordMapper.selectList(Wrappers.<ReadingRecord>query().eq("user_id", userId));
        long readingCount = readings.stream()
                .filter(record -> Boolean.TRUE.equals(record.finished))
                .count();
        long lookedUpWords = readings.stream()
                .mapToLong(record -> record.lookedUpWords == null ? 0 : record.lookedUpWords)
                .sum();
        return new LearningReportView(
                learnedWords,
                reviewWords,
                favoriteWords,
                essayCount,
                readingCount,
                painPoints(learnedWords, reviewWords, dueWords, favoriteWords, essayCount, readingCount, lookedUpWords),
                nextActions(learnedWords, reviewWords, dueWords, favoriteWords, essayCount, readingCount, lookedUpWords)
        );
    }

    private List<String> painPoints(
            long learnedWords,
            long reviewWords,
            long dueWords,
            long favoriteWords,
            long essayCount,
            long readingCount,
            long lookedUpWords
    ) {
        List<String> items = new ArrayList<>();
        if (reviewWords > 0) {
            items.add("有 " + reviewWords + " 个错词需要回炉复习");
        }
        if (dueWords > 0) {
            items.add("今天有 " + dueWords + " 个单词已经到复习时间");
        }
        if (learnedWords < 6) {
            items.add("已掌握词数量偏少，需要先稳定每日词汇输入");
        }
        if (readingCount == 0) {
            items.add("还没有完成阅读记录，阅读输入不足");
        } else if (lookedUpWords == 0) {
            items.add("阅读时没有记录查词，建议把生词沉淀到复习流程");
        }
        if (essayCount == 0) {
            items.add("还没有写作批改记录，输出练习不足");
        }
        if (favoriteWords == 0) {
            items.add("还没有收藏重点词，复习目标不够聚焦");
        }
        if (items.isEmpty()) {
            items.add("学习节奏稳定，下一步可以提高材料难度");
            items.add("继续扩大表达类词汇和写作连接词储备");
        }
        return items.stream().limit(3).toList();
    }

    private List<String> nextActions(
            long learnedWords,
            long reviewWords,
            long dueWords,
            long favoriteWords,
            long essayCount,
            long readingCount,
            long lookedUpWords
    ) {
        List<String> items = new ArrayList<>();
        if (dueWords > 0 || reviewWords > 0) {
            items.add("先完成错词和到期词复习");
        } else {
            items.add("完成 8 个今日单词并提交测验");
        }
        if (readingCount == 0) {
            items.add("阅读 1 篇 easy 新闻并查 3 个生词");
        } else if (lookedUpWords < 3) {
            items.add("在下一篇阅读中至少记录 3 个生词");
        } else {
            items.add("尝试阅读 1 篇更高难度新闻");
        }
        if (essayCount == 0) {
            items.add("提交 1 篇 80-120 词短作文批改");
        } else {
            items.add("根据最近批改建议重写 1 个段落");
        }
        if (favoriteWords == 0) {
            items.add("收藏 5 个高频词，建立重点复习清单");
        } else if (learnedWords >= 12) {
            items.add("用已收藏单词写 3 个新句子");
        }
        return items.stream().limit(4).toList();
    }
}
