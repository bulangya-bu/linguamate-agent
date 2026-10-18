package com.example.javabigwork.mission.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.javabigwork.vocabulary.entity.Word;
import com.example.javabigwork.vocabulary.mapper.WordMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class MissionWordSeeder implements ApplicationRunner {
    private final MissionCatalog catalog;
    private final WordMapper words;
    public MissionWordSeeder(MissionCatalog catalog,WordMapper words){this.catalog=catalog;this.words=words;}
    @Override public void run(ApplicationArguments args) {
        for(var lesson:catalog.all())for(var task:lesson.words()) {
            if(words.selectCount(Wrappers.<Word>query().eq("word",task.word()))>0)continue;
            var word=new Word();word.word=task.word();word.meaningCn=task.meaning();word.exampleEn=task.sentence();
            word.exampleCn=task.translation();word.levelTag="local-fast";words.insert(word);
        }
    }
}
