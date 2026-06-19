package com.example.javabigwork.agent;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AgentTraceRecorder {
    private static final Logger traceLog = LoggerFactory.getLogger("AGENT_TRACE");

    public List<AgentTraceStep> start(String userInput) {
        List<AgentTraceStep> trace = new ArrayList<>();
        trace.add(new AgentTraceStep("Observe", "接收输入", userInput));
        return trace;
    }

    public void add(List<AgentTraceStep> trace, String phase, String title, String detail) {
        trace.add(new AgentTraceStep(phase, title, detail));
    }

    public void flush(List<AgentTraceStep> trace, String sessionId) {
        traceLog.info("=== Agent Trace Session: {} ===", sessionId);
        for (AgentTraceStep step : trace) {
            traceLog.info("[{}] {} - {}", step.phase(), step.title(), step.detail());
        }
        traceLog.info("=== End Trace: {} ===", sessionId);
    }
}
