package com.bookstore.agent.core;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * A request to invoke a named tool with validated arguments.
 */
@Data
@Builder
public class ToolCallRequest {
    private String toolName;
    private Map<String, Object> arguments;
    private String correlationId;
}
