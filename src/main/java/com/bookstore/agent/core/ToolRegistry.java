package com.bookstore.agent.core;

import com.bookstore.exception.BookstoreException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Central registry for all agent tools.
 * Tools self-register at startup via {@link ToolRegistrar}.
 */
@Component
@Slf4j
public class ToolRegistry {

    private final Map<String, ToolDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, Function<ToolCallRequest, ToolCallResult>> executors = new ConcurrentHashMap<>();

    public void register(ToolDefinition definition, Function<ToolCallRequest, ToolCallResult> executor) {
        definitions.put(definition.getName(), definition);
        executors.put(definition.getName(), executor);
        log.debug("Registered tool: {}", definition.getName());
    }

    public ToolDefinition getDefinition(String toolName) {
        ToolDefinition def = definitions.get(toolName);
        if (def == null) throw BookstoreException.notFound("Tool", toolName);
        return def;
    }

    public Function<ToolCallRequest, ToolCallResult> getExecutor(String toolName) {
        Function<ToolCallRequest, ToolCallResult> exec = executors.get(toolName);
        if (exec == null) throw BookstoreException.notFound("Tool", toolName);
        return exec;
    }

    public Collection<ToolDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public List<ToolDefinition> getDefinitionsForAgent(String agentType) {
        return definitions.values().stream().toList();
    }

    public boolean hasToolName(String toolName) {
        return definitions.containsKey(toolName);
    }
}
