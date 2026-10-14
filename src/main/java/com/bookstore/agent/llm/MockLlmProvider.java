package com.bookstore.agent.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Mock LLM provider for testing without a real API key.
 *
 * Behavior:
 *  - Detects keywords in the user message and calls the appropriate tool.
 *  - Returns structured tool call responses.
 *  - On second invocation (after tool results), returns a text response.
 *
 * This is NOT a keyword chatbot — it is a mock of the structured tool-selection
 * flow that a real LLM would perform. The tools still invoke real application
 * services with validated arguments.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "bookstore.agent.mock-mode", havingValue = "true", matchIfMissing = true)
public class MockLlmProvider implements LlmProvider {

    @Override
    public LlmResponse chat(LlmRequest request) {
        log.info("[MOCK LLM] Processing request with {} messages", request.getMessages().size());

        // If the last message is a tool result, provide a final text response
        boolean hasToolResult = request.getMessages().stream()
            .anyMatch(m -> "tool".equals(m.getRole()));

        if (hasToolResult) {
            return LlmResponse.builder()
                .type(LlmResponse.ResponseType.TEXT)
                .text(generateTextFromToolResults(request))
                .build();
        }

        // Determine what tool to call based on the user message
        String userMessage = extractUserMessage(request).toLowerCase();
        List<ToolDefinition> tools = request.getAvailableTools();

        if (tools == null || tools.isEmpty()) {
            return LlmResponse.builder()
                .type(LlmResponse.ResponseType.TEXT)
                .text("I understand your request but I have no tools available to help with that.")
                .build();
        }

        // Route to appropriate tools based on message content
        List<LlmResponse.ToolCall> toolCalls = selectTools(userMessage, tools);

        if (!toolCalls.isEmpty()) {
            return LlmResponse.builder()
                .type(LlmResponse.ResponseType.TOOL_CALLS)
                .toolCalls(toolCalls)
                .build();
        }

        return LlmResponse.builder()
            .type(LlmResponse.ResponseType.TEXT)
            .text("I can help you with bookstore queries. Please ask about books, orders, inventory, or get recommendations.")
            .build();
    }

    private List<LlmResponse.ToolCall> selectTools(String message, List<ToolDefinition> tools) {
        List<LlmResponse.ToolCall> calls = new ArrayList<>();

        // Book search patterns
        if (message.contains("search") || message.contains("find") || message.contains("look for") ||
            message.contains("show me") || message.contains("books about") || message.contains("java") ||
            message.contains("python") || message.contains("programming")) {
            findTool(tools, "searchBooks").ifPresent(tool -> {
                Map<String, Object> args = new HashMap<>();
                args.put("query", extractQueryFromMessage(message));
                if (message.contains("under") || message.contains("₹") || message.contains("price")) {
                    args.put("maxPrice", extractPrice(message));
                }
                calls.add(buildToolCall(tool.getName(), args));
            });
        }

        // Order queries
        if (message.contains("order") || message.contains("purchase") || message.contains("bought")) {
            findTool(tools, "getMyOrders").ifPresent(tool ->
                calls.add(buildToolCall(tool.getName(), new HashMap<>())));
        }

        // Inventory queries
        if (message.contains("inventory") || message.contains("stock") || message.contains("low")) {
            findTool(tools, "getLowStockBooks").ifPresent(tool -> {
                Map<String, Object> args = new HashMap<>();
                args.put("threshold", 10);
                calls.add(buildToolCall(tool.getName(), args));
            });
        }

        // Recommendations
        if (message.contains("recommend") || message.contains("suggestion") || message.contains("similar")) {
            findTool(tools, "searchBooks").ifPresent(tool -> {
                Map<String, Object> args = new HashMap<>();
                args.put("query", "popular");
                calls.add(buildToolCall(tool.getName(), args));
            });
        }

        // Top rated
        if (message.contains("top rated") || message.contains("best") || message.contains("popular")) {
            findTool(tools, "getTopRatedBooks").ifPresent(tool -> {
                Map<String, Object> args = new HashMap<>();
                args.put("limit", 5);
                calls.add(buildToolCall(tool.getName(), args));
            });
        }

        // Shipping/return policy
        if (message.contains("shipping") || message.contains("delivery")) {
            findTool(tools, "getShippingPolicy").ifPresent(tool ->
                calls.add(buildToolCall(tool.getName(), new HashMap<>())));
        }
        if (message.contains("return") || message.contains("refund")) {
            findTool(tools, "getReturnPolicy").ifPresent(tool ->
                calls.add(buildToolCall(tool.getName(), new HashMap<>())));
        }

        return calls;
    }

    private String generateTextFromToolResults(LlmRequest request) {
        String userMessage = extractUserMessage(request).toLowerCase();

        // Collect tool results
        StringBuilder toolContext = new StringBuilder();
        request.getMessages().stream()
            .filter(m -> "tool".equals(m.getRole()))
            .forEach(m -> toolContext.append(m.getContent()).append("\n"));

        if (toolContext.length() == 0) {
            return "I processed your request but could not find relevant information.";
        }

        if (userMessage.contains("search") || userMessage.contains("find") || userMessage.contains("java")) {
            return "Here are the books I found based on your query. The results include details on pricing, availability, and ratings. " +
                "Please review the catalog results above. If you need more specific recommendations, let me know your preferences.";
        }
        if (userMessage.contains("order")) {
            return "Here are your recent orders. If you need to cancel an order or have questions about shipping, please let me know.";
        }
        if (userMessage.contains("inventory") || userMessage.contains("stock")) {
            return "Here is the current inventory status. Items with low stock are flagged for restocking.";
        }
        if (userMessage.contains("recommend")) {
            return "Based on your preferences, here are some books you might enjoy. These recommendations are drawn from our current catalog.";
        }
        if (userMessage.contains("shipping")) {
            return "Here is our shipping policy. For specific order tracking, please provide your order number.";
        }

        return "I've retrieved the requested information from our system. Please review the results above.";
    }

    private String extractUserMessage(LlmRequest request) {
        return request.getMessages().stream()
            .filter(m -> "user".equals(m.getRole()))
            .reduce((first, second) -> second)
            .map(LlmRequest.Message::getContent)
            .orElse("");
    }

    private Optional<ToolDefinition> findTool(List<ToolDefinition> tools, String name) {
        return tools.stream().filter(t -> t.getName().equals(name)).findFirst();
    }

    private LlmResponse.ToolCall buildToolCall(String toolName, Map<String, Object> args) {
        return LlmResponse.ToolCall.builder()
            .id("call-" + UUID.randomUUID().toString().substring(0, 8))
            .toolName(toolName)
            .arguments(args)
            .build();
    }

    private String extractQueryFromMessage(String message) {
        // Simple extraction: remove common stop words and return the core
        String[] cleanWords = {"find", "search", "show", "me", "books", "about", "for", "the",
            "a", "an", "i", "want", "please", "need", "some", "any"};
        String result = message;
        for (String word : cleanWords) {
            result = result.replaceAll("\\b" + word + "\\b", " ");
        }
        result = result.replaceAll("under ₹?\\d+", "").trim().replaceAll("\\s+", " ").trim();
        return result.isEmpty() ? "programming" : result;
    }

    private Double extractPrice(String message) {
        // Extract numeric price from strings like "under ₹1000", "below 500"
        var matcher = java.util.regex.Pattern.compile("(?:₹|rs\\.?\\s*|price\\s*)?([0-9]+(?:\\.[0-9]+)?)")
            .matcher(message);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : 1000.0;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getProviderName() {
        return "mock-llm";
    }
}
