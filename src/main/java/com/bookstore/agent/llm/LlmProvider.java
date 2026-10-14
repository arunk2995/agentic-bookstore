package com.bookstore.agent.llm;

import java.util.List;

/**
 * Abstraction over an LLM that can select and invoke tools.
 */
public interface LlmProvider {

    /**
     * Send a conversation to the LLM and get back a response that may include
     * tool call selections or a final text response.
     */
    LlmResponse chat(LlmRequest request);

    /**
     * Whether this provider is available (e.g., API key configured, service reachable).
     */
    boolean isAvailable();

    String getProviderName();
}
