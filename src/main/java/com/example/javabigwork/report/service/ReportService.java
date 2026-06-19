package com.example.javabigwork.report.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.news.mapper.ReadingRecordMapper;
import com.example.javabigwork.report.dto.LearningReportView;
import com.example.javabigwork.vocabulary.entity.UserWordProgress;
import com.example.javabigwork.vocabulary.mapper.UserWordProgressMapper;
import com.example.javabigwork.writing.mapper.EssaySubmissionMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final UserWordProgressMapper progressMapper;
    private final EssaySubmissionMapper essayMapper;
    private final ReadingRecordMapper readingRecordMapper;

    public ReportService(
            UserWordProgressMapper progressMapper,
            EssaySubmissionMapper essayMapper,
            ReadingRecordMapper readingRecordMapper
    ) {
        this.progressMapper = progressMapper;
        this.essayMapper = essayMapper;
        this.readingRecordMapper = readingRecordMapper;
    }

    public LearningReportView summary() {
        long learnedWords = progressMapper.selectCount(Wrappers.<UserWordProgress>query().ge("familiarity", 3));
        long reviewWords = progressMapper.selectCount(Wrappers.<UserWordProgress>query().gt("wrong_count", 0));
        long favoriteWords = progressMapper.selectCount(Wrappers.<UserWordProgress>query().eq("favorite", true));
        long essayCount = essayMapper.selectCount(Wrappers.query());
        long readingCount = readingRecordMapper.selectCount(Wrappers.query());
        return new LearningReportView(
                learnedWords,
                reviewWords,
                favoriteWords,
                essayCount,
                readingCount,
                List.of("连接词使用偏少", "部分高频词仍需复习", "阅读时可记录更多上下文例句"),
                List.of("完成 6 个今日单词", "复习错词队列", "阅读 1 篇 easy 新闻", "提交 1 篇短作文批改")
        );
    }
}
