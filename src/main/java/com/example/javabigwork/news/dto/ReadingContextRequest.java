package com.example.javabigwork.news.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReadingContextRequest(@NotBlank @Size(max = 64) String word,
                                    @NotBlank @Size(max = 1200) String sentence) {
}
