package com.example.javabigwork.writing.service;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EssayRubricService {

    public List<String> rubricItems(String examType) {
        return List.of(
                "Grammar accuracy：时态、主谓一致、冠词、介词使用",
                "Vocabulary range：词汇准确性、多样性和搭配自然度",
                "Structure：开头、主体、结尾和段落衔接",
                "Logic：观点是否清楚，例子是否支持论点"
        );
    }
}
