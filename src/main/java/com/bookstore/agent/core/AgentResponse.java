package com.bookstore.agent.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentResponse {
    private String correlationId;
    private String agentType;
    private String message;
    private int toolCallsCount;
    private long durationMs;
}
