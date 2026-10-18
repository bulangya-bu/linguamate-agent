package com.example.javabigwork.writing.dto;

public record RevisionComparison(Long previousEssayId, Double previousScore, double currentScore,
                                 boolean comparable, String note) {
}
