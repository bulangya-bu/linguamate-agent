package com.example.javabigwork.vocabulary.dto;

public record WordBankStats(long all, long review, long favorites, long mastered,long due) {
    public WordBankStats(long all,long review,long favorites,long mastered){this(all,review,favorites,mastered,0);}
}
