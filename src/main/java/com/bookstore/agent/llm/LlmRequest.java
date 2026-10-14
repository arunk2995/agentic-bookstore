package com.bookstore.agent.llm;

import com.bookstore.agent.core.ToolDefinition;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class LlmRequest {
    private List<Message> messages;
    private List<ToolDefinition> availableTools;
    private String systemPrompt;
    private int maxTokens;

    @Data
    @Builder
    public static class Message {
        private String role;  // user, assistant, tool
        private String content;
        private String toolCallId;
        private String toolName;
    }
}
