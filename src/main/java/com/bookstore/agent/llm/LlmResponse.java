package com.bookstore.agent.llm;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class LlmResponse {
    private ResponseType type;
    private String text;
    private List<ToolCall> toolCalls;

    public enum ResponseType {
        TEXT,       // Final response
        TOOL_CALLS  // LLM wants to call tools
    }

    @Data
    @Builder
    public static class ToolCall {
        private String id;
        private String toolName;
        private Map<String, Object> arguments;
    }
}
