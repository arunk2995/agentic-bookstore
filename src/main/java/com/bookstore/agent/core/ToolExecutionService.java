package com.bookstore.agent.core;

import com.bookstore.domain.agent.*;
import com.bookstore.exception.BookstoreException;
import com.bookstore.repository.AgentConversationRepository;
import com.bookstore.repository.AgentToolExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Executes a single tool call with authorization, validation, timing, and audit logging.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ToolExecutionService {

    private final ToolRegistry toolRegistry;
    private final AgentConversationRepository conversationRepository;
    private final AgentToolExecutionRepository executionRepository;

    @Transactional
    public ToolCallResult execute(ToolCallRequest request, UUID conversationId, UUID userId,
                                   boolean isAdmin, List<String> userRoles) {
        ToolDefinition def = toolRegistry.getDefinition(request.getToolName());

        // Authorization check
        if (def.isRequiresAuth() && userId == null) {
            return ToolCallResult.error("UNAUTHORIZED", "Authentication required for tool: " + request.getToolName());
        }

        if (!def.getRequiredRoles().isEmpty()) {
            boolean hasRole = def.getRequiredRoles().stream()
                .anyMatch(role -> userRoles.contains("ROLE_" + role) || userRoles.contains(role));
            if (!hasRole && !isAdmin) {
                return ToolCallResult.error("FORBIDDEN",
                    "Insufficient permissions for tool: " + request.getToolName());
            }
        }

        // Argument validation
        var validationError = validateArguments(def, request.getArguments());
        if (validationError != null) {
            return ToolCallResult.error("VALIDATION_ERROR", validationError);
        }

        long startTime = System.currentTimeMillis();
        ToolCallResult result;

        try {
            result = toolRegistry.getExecutor(request.getToolName()).apply(request);
        } catch (BookstoreException e) {
            result = ToolCallResult.error(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Tool execution error for {}: {}", request.getToolName(), e.getMessage(), e);
            result = ToolCallResult.error("TOOL_ERROR", "Tool execution failed: " + e.getMessage());
        }

        long durationMs = System.currentTimeMillis() - startTime;
        result.setDurationMs(durationMs);

        // Audit log
        try {
            AgentToolExecution execution = AgentToolExecution.builder()
                .conversationId(conversationId)
                .correlationId(request.getCorrelationId())
                .toolName(request.getToolName())
                .arguments(safeSerialize(request.getArguments()))
                .resultSummary(result.toSummary())
                .success(result.isSuccess())
                .errorMessage(result.getErrorMessage())
                .durationMs(durationMs)
                .userId(userId)
                .build();
            executionRepository.save(execution);
        } catch (Exception e) {
            log.warn("Failed to save tool execution audit log: {}", e.getMessage());
        }

        log.debug("Tool {} executed in {}ms success={}", request.getToolName(), durationMs, result.isSuccess());
        return result;
    }

    private String validateArguments(ToolDefinition def, Map<String, Object> args) {
        if (def.getParameters() == null) return null;
        for (Map.Entry<String, ToolDefinition.ToolParameter> entry : def.getParameters().entrySet()) {
            String paramName = entry.getKey();
            ToolDefinition.ToolParameter param = entry.getValue();
            if (param.isRequired() && (args == null || !args.containsKey(paramName) || args.get(paramName) == null)) {
                return "Required parameter missing: " + paramName;
            }
        }
        return null;
    }

    private String safeSerialize(Object obj) {
        if (obj == null) return null;
        try {
            return obj.toString();
        } catch (Exception e) {
            return "[serialization error]";
        }
    }
}
