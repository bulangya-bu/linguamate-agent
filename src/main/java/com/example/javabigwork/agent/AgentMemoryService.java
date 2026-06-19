package com.example.javabigwork.agent;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentMemoryService {

    public List<String> defaultSuggestions() {
        return List.of(
                "把今天答错的单词加入复习队列",
                "用一个新单词造句并让 Agent 修改",
                "阅读一篇 easy 难度新闻并记录生词"
        );
    }
}
