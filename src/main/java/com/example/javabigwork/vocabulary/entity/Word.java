package com.example.javabigwork.vocabulary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("word")
public class Word {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String word;
    public String phoneticUs;
    public String phoneticUk;
    public String meaningCn;
    public String partOfSpeech;
    public String exampleEn;
    public String exampleCn;
    public String levelTag;
}
