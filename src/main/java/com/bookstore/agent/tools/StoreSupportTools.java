package com.bookstore.agent.tools;

import com.bookstore.agent.core.*;
import com.bookstore.domain.support.SupportTicket;
import com.bookstore.domain.support.TicketStatus;
import com.bookstore.repository.SupportTicketRepository;
import com.bookstore.security.BookstoreUserPrincipal;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tools for the Store Support Agent.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StoreSupportTools {

    private final ToolRegistry toolRegistry;
    private final SupportTicketRepository ticketRepository;

    private static final String SHIPPING_POLICY = """
        SHIPPING POLICY:
        - Standard delivery: 5-7 business days, FREE on orders above ₹500
        - Express delivery: 2-3 business days, ₹99
        - Same-day delivery: Available in select cities, ₹199
        - International shipping: 10-15 business days, rates vary
        - All books are carefully packaged to prevent damage.
        """;

    private static final String RETURN_POLICY = """
        RETURN POLICY:
        - Returns accepted within 30 days of delivery
        - Books must be in original, undamaged condition
        - Defective or incorrect items: Free return and replacement
        - Change of mind returns: Customer bears return shipping cost
        - Refund processed within 5-7 business days
        - Digital downloads are non-refundable
        """;

    @PostConstruct
    public void registerTools() {
        toolRegistry.register(
            ToolDefinition.builder()
                .name("getShippingPolicy")
                .description("Get the bookstore's shipping policy and rates")
                .parameters(Map.of())
                .requiredRoles(List.of())
                .requiresAuth(false)
                .build(),
            r -> ToolCallResult.success(Map.of("policy", SHIPPING_POLICY))
        );

        toolRegistry.register(
            ToolDefinition.builder()
                .name("getReturnPolicy")
                .description("Get the bookstore's return and refund policy")
                .parameters(Map.of())
                .requiredRoles(List.of())
                .requiresAuth(false)
                .build(),
            r -> ToolCallResult.success(Map.of("policy", RETURN_POLICY))
        );

        toolRegistry.register(
            ToolDefinition.builder()
                .name("createSupportTicket")
                .description("Create a support ticket for a customer issue")
                .parameters(Map.of(
                    "subject", ToolDefinition.ToolParameter.builder().type("string").description("Ticket subject").required(true).build(),
                    "description", ToolDefinition.ToolParameter.builder().type("string").description("Detailed description").required(true).build(),
                    "category", ToolDefinition.ToolParameter.builder().type("string").description("Category: ORDER, PAYMENT, SHIPPING, GENERAL").required(false).build()
                ))
                .requiredRoles(List.of())
                .requiresAuth(false)
                .build(),
            this::createSupportTicket
        );
    }

    private ToolCallResult createSupportTicket(ToolCallRequest request) {
        Map<String, Object> args = request.getArguments();
        String subject = getStr(args, "subject");
        String description = getStr(args, "description");
        String category = getStr(args, "category");

        if (subject == null) return ToolCallResult.error("MISSING_PARAM", "subject required");
        if (description == null) return ToolCallResult.error("MISSING_PARAM", "description required");

        // Get authenticated user if available
        UUID userId = null;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof BookstoreUserPrincipal p) {
            userId = p.getUserId();
        }

        String ticketNumber = "TKT-" + System.currentTimeMillis();

        SupportTicket ticket = SupportTicket.builder()
            .ticketNumber(ticketNumber)
            .subject(subject)
            .description(description)
            .category(category != null ? category : "GENERAL")
            .status(TicketStatus.OPEN)
            .build();

        ticket = ticketRepository.save(ticket);

        return ToolCallResult.success(Map.of(
            "ticketId", ticket.getId(),
            "ticketNumber", ticket.getTicketNumber(),
            "status", "OPEN",
            "message", "Support ticket created. Our team will respond within 24 hours."
        ));
    }

    private String getStr(Map<String, Object> args, String key) {
        if (args == null) return null;
        Object v = args.get(key);
        return v != null ? v.toString() : null;
    }
}
