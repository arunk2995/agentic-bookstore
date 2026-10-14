package com.bookstore.agent;

import com.bookstore.agent.core.*;
import com.bookstore.agent.llm.*;
import com.bookstore.config.BookstoreProperties;
import com.bookstore.domain.agent.AgentConversation;
import com.bookstore.domain.agent.ConversationStatus;
import com.bookstore.repository.AgentConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentOrchestrator Unit Tests")
class AgentOrchestratorTest {

    @Mock LlmProvider llmProvider;
    @Mock ToolRegistry toolRegistry;
    @Mock ToolExecutionService toolExecutionService;
    @Mock AgentConversationRepository conversationRepository;
    @Mock BookstoreProperties properties;

    @InjectMocks AgentOrchestrator orchestrator;

    @BeforeEach
    void setup() {
        var agentProps = new BookstoreProperties.Agent();
        agentProps.setMaxIterations(5);
        agentProps.setMaxTokens(4096);
        when(properties.getAgent()).thenReturn(agentProps);

        AgentConversation savedConv = AgentConversation.builder()
            .id(UUID.randomUUID())
            .correlationId("test-corr")
            .status(ConversationStatus.STARTED)
            .agentType("book-discovery")
            .build();
        when(conversationRepository.save(any())).thenReturn(savedConv);
        when(toolRegistry.getAllDefinitions()).thenReturn(List.of());
    }

    @Test
    @DisplayName("Agent returns TEXT response when LLM responds with text directly")
    void agent_llmTextResponse_returnsDirectly() {
        when(llmProvider.chat(any())).thenReturn(
            LlmResponse.builder()
                .type(LlmResponse.ResponseType.TEXT)
                .text("Here are some Java books for you.")
                .build()
        );

        AgentRequest request = new AgentRequest();
        request.setMessage("Find Java books");
        request.setAgentType("book-discovery");

        AgentResponse response = orchestrator.run(request, null);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Here are some Java books for you.");
        assertThat(response.getToolCallsCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Agent executes tool calls when LLM returns TOOL_CALLS")
    void agent_llmToolCalls_executesTools() {
        LlmResponse toolCallResponse = LlmResponse.builder()
            .type(LlmResponse.ResponseType.TOOL_CALLS)
            .toolCalls(List.of(LlmResponse.ToolCall.builder()
                .id("call-1")
                .toolName("searchBooks")
                .arguments(Map.of("query", "java"))
                .build()))
            .build();

        LlmResponse textResponse = LlmResponse.builder()
            .type(LlmResponse.ResponseType.TEXT)
            .text("I found 5 Java books.")
            .build();

        when(llmProvider.chat(any()))
            .thenReturn(toolCallResponse)
            .thenReturn(textResponse);

        when(toolExecutionService.execute(any(), any(), any(), anyBoolean(), any()))
            .thenReturn(ToolCallResult.success(List.of("Book 1", "Book 2")));

        AgentRequest request = new AgentRequest();
        request.setMessage("Find Java books");
        request.setAgentType("book-discovery");

        AgentResponse response = orchestrator.run(request, null);

        assertThat(response.getToolCallsCount()).isEqualTo(1);
        assertThat(response.getMessage()).isEqualTo("I found 5 Java books.");
    }

    @Test
    @DisplayName("Tool execution error is handled gracefully")
    void agent_toolExecutionError_handledGracefully() {
        LlmResponse toolCallResponse = LlmResponse.builder()
            .type(LlmResponse.ResponseType.TOOL_CALLS)
            .toolCalls(List.of(LlmResponse.ToolCall.builder()
                .id("call-err")
                .toolName("searchBooks")
                .arguments(Map.of())
                .build()))
            .build();

        LlmResponse textResponse = LlmResponse.builder()
            .type(LlmResponse.ResponseType.TEXT)
            .text("I encountered an issue searching for books.")
            .build();

        when(llmProvider.chat(any()))
            .thenReturn(toolCallResponse)
            .thenReturn(textResponse);

        when(toolExecutionService.execute(any(), any(), any(), anyBoolean(), any()))
            .thenReturn(ToolCallResult.error("SEARCH_ERROR", "Search failed"));

        AgentRequest request = new AgentRequest();
        request.setMessage("Find books");
        request.setAgentType("book-discovery");

        AgentResponse response = orchestrator.run(request, null);
        assertThat(response).isNotNull(); // no exception
    }
}
