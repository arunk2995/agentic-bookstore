package com.bookstore.agent.core;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Immutable descriptor for a registered tool.
 */
@Data
@Builder
public class ToolDefinition {
    private String name;
    private String description;
    private Map<String, ToolParameter> parameters;
    private List<String> requiredRoles;
    private boolean requiresAuth;
    private boolean requiresConfirmation;

    @Data
    @Builder
    public static class ToolParameter {
        private String type;        // string, number, boolean, integer
        private String description;
        private boolean required;
        private List<String> enumValues;
        private Double minimum;
        private Double maximum;
    }
}
