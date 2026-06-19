package com.example.javabigwork.news.dto;

public record HoverWordResponse(
        String word,
        String phoneticUs,
        String phoneticUk,
        String meaningCn,
        String partOfSpeech,
        String exampleEn,
        boolean savedInReviewBook
) {
}
