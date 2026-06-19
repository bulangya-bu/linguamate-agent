package com.example.javabigwork.news.service;

import com.example.javabigwork.news.dto.HoverWordResponse;
import com.example.javabigwork.tool.DictionaryTool;
import org.springframework.stereotype.Service;

@Service
public class ReadingAssistService {
    private final DictionaryTool dictionaryTool;

    public ReadingAssistService(DictionaryTool dictionaryTool) {
        this.dictionaryTool = dictionaryTool;
    }

    public HoverWordResponse lookup(String word) {
        return dictionaryTool.lookup(word);
    }
}
