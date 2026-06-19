package com.example.javabigwork.agent;

import com.example.javabigwork.chat.entity.Message;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LinguaMateAgent {
    private final AgentOrchestrator orchestrator;

    public LinguaMateAgent(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public AgentResult chat(String scene, String message) {
        return orchestrator.handle(scene, message, List.of());
    }

    public AgentResult chat(String scene, String message, List<Message> history) {
        return orchestrator.handle(scene, message, history);
    }
}
