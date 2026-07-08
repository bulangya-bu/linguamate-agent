package com.example.javabigwork.news.service;

import com.example.javabigwork.news.dto.HoverWordResponse;
import com.example.javabigwork.tool.DictionaryTool;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReadingAssistService {
    private final DictionaryTool dictionaryTool;

    public ReadingAssistService(DictionaryTool dictionaryTool) {
        this.dictionaryTool = dictionaryTool;
    }

    public HoverWordResponse lookup(String word) {
        String normalized = word == null ? "" : word.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("请输入要查询的单词");
        }
        if (normalized.length() > 64) {
            throw new IllegalArgumentException("查询单词不能超过 64 个字符");
        }
        return dictionaryTool.lookup(normalized);
    }
}
