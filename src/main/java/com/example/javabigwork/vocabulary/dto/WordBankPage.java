package com.example.javabigwork.vocabulary.dto;

import java.util.List;

public record WordBankPage(
        List<WordCardView> words,
        long total,
        int page,
        int size
) {}
