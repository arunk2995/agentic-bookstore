package com.bookstore.agent.core;

import com.bookstore.agent.llm.*;
import com.bookstore.config.BookstoreProperties;
import com.bookstore.domain.agent.*;
import com.bookstore.repository.AgentConversationRepository;
import com.bookstore.security.BookstoreUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates a full agent interaction:
 *   User message → LLM → Tool calls → Tool results → LLM → Final response
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final LlmProvider llmProvider;
    private final ToolRegistry toolRegistry;
    private final ToolExecutionService toolExecutionService;
    private final AgentConversationRepository conversationRepository;
    private final BookstoreProperties properties;

    @Transactional
    public AgentResponse run(AgentRequest request, BookstoreUserPrincipal principal) {
        String correlationId = request.getCorrelationId() != null ?
            request.getCorrelationId() : UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        UUID userId = principal != null ? principal.getUserId() : null;
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        List<String> userRoles = principal != null ?
            principal.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList()) :
            List.of();

        // Create conversation record
        AgentConversation conversation = AgentConversation.builder()
            .correlationId(correlationId)
            .user(null) // set separately to avoid lazy loading issues
            .agentType(request.getAgentType())
            .userMessage(request.getMessage())
            .status(ConversationStatus.STARTED)
            .build();
        conversation = conversationRepository.save(conversation);
        final UUID conversationId = conversation.getId();

        List<LlmRequest.Message> messages = new ArrayList<>();
        messages.add(LlmRequest.Message.builder()
            .role("user")
            .content(request.getMessage())
            .build());

        List<ToolDefinition> tools = getToolsForAgent(request.getAgentType());
        String systemPrompt = buildSystemPrompt(request.getAgentType());

        String finalResponse = null;
        int iterations = 0;
        int maxIterations = properties.getAgent().getMaxIterations();
        int toolCallsCount = 0;

        try {
            while (iterations < maxIterations) {
                iterations++;
                log.debug("[{}] Agent iteration {} for agent type {}", correlationId, iterations, request.getAgentType());

                LlmRequest llmRequest = LlmRequest.builder()
                    .messages(new ArrayList<>(messages))
                    .availableTools(tools)
                    .systemPrompt(systemPrompt)
                    .maxTokens(properties.getAgent().getMaxTokens())
                    .build();

                LlmResponse llmResponse = llmProvider.chat(llmRequest);

                if (llmResponse.getType() == LlmResponse.ResponseType.TEXT) {
                    finalResponse = llmResponse.getText();
                    break;
                }

                // Execute tool calls
                if (llmResponse.getToolCalls() != null && !llmResponse.getToolCalls().isEmpty()) {
                    for (LlmResponse.ToolCall toolCall : llmResponse.getToolCalls()) {
                        toolCallsCount++;

                        ToolCallRequest tcRequest = ToolCallRequest.builder()
                            .toolName(toolCall.getToolName())
                            .arguments(toolCall.getArguments())
                            .correlationId(correlationId)
                            .build();

                        ToolCallResult result = toolExecutionService.execute(
                            tcRequest, conversationId, userId, isAdmin, userRoles);

                        // Add tool result to conversation history
                        String resultContent = result.isSuccess() ?
                            serializeResult(result.getData()) :
                            "Error: " + result.getErrorMessage();

                        messages.add(LlmRequest.Message.builder()
                            .role("tool")
                            .toolCallId(toolCall.getId())
                            .toolName(toolCall.getToolName())
                            .content(resultContent)
                            .build());
                    }
                }
            }

            if (finalResponse == null) {
                finalResponse = "I've processed your request through " + toolCallsCount + " tool calls. " +
                    "Please review the results above.";
            }

            // Update conversation record
            conversation.setAgentResponse(finalResponse);
            conversation.setToolCallsCount(toolCallsCount);
            conversation.setDurationMs(System.currentTimeMillis() - startTime);
            conversation.setStatus(ConversationStatus.COMPLETED);
            conversationRepository.save(conversation);

        } catch (Exception e) {
            log.error("[{}] Agent error: {}", correlationId, e.getMessage(), e);
            finalResponse = "I encountered an error processing your request. Please try again.";
            conversation.setStatus(ConversationStatus.FAILED);
            conversation.setErrorMessage(e.getMessage());
            conversation.setDurationMs(System.currentTimeMillis() - startTime);
            conversationRepository.save(conversation);
        }

        return AgentResponse.builder()
            .correlationId(correlationId)
            .agentType(request.getAgentType())
            .message(finalResponse)
            .toolCallsCount(toolCallsCount)
            .durationMs(System.currentTimeMillis() - startTime)
            .build();
    }

    private List<ToolDefinition> getToolsForAgent(String agentType) {
        // Filter tools by agent type — each agent has a curated set
        Set<String> agentTools = switch (agentType) {
            case "book-discovery" -> Set.of("searchBooks", "getBookDetails", "getBooksByAuthor",
                "getRelatedBooks", "getBookAvailability", "getTopRatedBooks");
            case "recommendations" -> Set.of("searchBooks", "getRelatedBooks", "getBookDetails",
                "getTopRatedBooks");
            case "order-assistance" -> Set.of("getMyOrders", "getMyOrderDetails",
                "getOrderStatus", "requestOrderCancellation");
            case "inventory-assistance" -> Set.of("getInventory", "getLowStockBooks",
                "getInventoryMovements", "proposeStockAdjustment");
            case "store-support" -> Set.of("searchBooks", "getShippingPolicy", "getReturnPolicy",
                "getOrderStatus", "createSupportTicket");
            default -> Set.of();
        };

        return toolRegistry.getAllDefinitions().stream()
            .filter(def -> agentTools.contains(def.getName()))
            .toList();
    }

    private String buildSystemPrompt(String agentType) {
        return switch (agentType) {
            case "book-discovery" -> """
                You are a helpful bookstore assistant that helps customers discover books.
                Use the available tools to search the actual catalog. Never invent books, prices, or availability.
                Always use tool results as the source of truth. Be specific and helpful.
                """;
            case "recommendations" -> """
                You are a personalized book recommendation agent.
                Base all recommendations on actual catalog data from tool results.
                Explain why each book matches the customer's interests.
                Never recommend books not found in the catalog.
                """;
            case "order-assistance" -> """
                You are an order assistance agent. You can only access orders belonging to the authenticated customer.
                Never accept customer IDs from the conversation — always use the authenticated user's identity.
                Do not claim an order was cancelled until the backend confirms it.
                """;
            case "inventory-assistance" -> """
                You are an inventory management assistant for authorized staff.
                Analyze inventory data and suggest replenishment. Never modify stock directly —
                only propose adjustments that require explicit confirmation.
                """;
            case "store-support" -> """
                You are a bookstore customer support agent.
                Answer questions based on actual store policies and catalog data from tools.
                If you cannot answer reliably, offer to create a support ticket.
                """;
            default -> "You are a helpful bookstore assistant.";
        };
    }

    private String serializeResult(Object data) {
        if (data == null) return "null";
        try {
            return data.toString();
        } catch (Exception e) {
            return "[result]";
        }
    }
}
