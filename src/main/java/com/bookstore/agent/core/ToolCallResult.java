package com.bookstore.agent.core;

import lombok.Builder;
import lombok.Data;

/**
 * Structured result from tool execution.
 */
@Data
@Builder
public class ToolCallResult {
    private boolean success;
    private Object data;
    private String errorCode;
    private String errorMessage;
    private long durationMs;

    public static ToolCallResult success(Object data) {
        return ToolCallResult.builder().success(true).data(data).build();
    }

    public static ToolCallResult error(String errorCode, String message) {
        return ToolCallResult.builder().success(false).errorCode(errorCode).errorMessage(message).build();
    }

    public String toSummary() {
        if (success) {
            return "success: " + (data != null ? data.toString().substring(0, Math.min(200, data.toString().length())) : "null");
        }
        return "error[" + errorCode + "]: " + errorMessage;
    }
}
