package com.example.javabigwork.agent;

import java.util.List;

public record AgentResult(String reply, List<AgentTraceStep> trace, List<String> suggestions) {
}
