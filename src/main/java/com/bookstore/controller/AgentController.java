package com.bookstore.controller;

import com.bookstore.agent.core.*;
import com.bookstore.security.BookstoreUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Tag(name = "Agents", description = "Agentic AI endpoints for natural language interaction")
public class AgentController {

    private final AgentOrchestrator agentOrchestrator;

    @PostMapping("/book-discovery")
    @Operation(summary = "Book Discovery Agent — find books using natural language")
    public ResponseEntity<AgentResponse> bookDiscovery(
            @Valid @RequestBody AgentRequest request,
            @AuthenticationPrincipal BookstoreUserPrincipal principal) {
        request.setAgentType("book-discovery");
        return ResponseEntity.ok(agentOrchestrator.run(request, principal));
    }

    @PostMapping("/recommendations")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Book Recommendation Agent — personalized book recommendations")
    public ResponseEntity<AgentResponse> recommendations(
            @Valid @RequestBody AgentRequest request,
            @AuthenticationPrincipal BookstoreUserPrincipal principal) {
        request.setAgentType("recommendations");
        return ResponseEntity.ok(agentOrchestrator.run(request, principal));
    }

    @PostMapping("/order-assistance")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Order Assistance Agent — help with your orders")
    public ResponseEntity<AgentResponse> orderAssistance(
            @Valid @RequestBody AgentRequest request,
            @AuthenticationPrincipal BookstoreUserPrincipal principal) {
        request.setAgentType("order-assistance");
        return ResponseEntity.ok(agentOrchestrator.run(request, principal));
    }

    @PostMapping("/inventory-assistance")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    @Operation(summary = "Inventory Operations Agent — staff inventory management")
    public ResponseEntity<AgentResponse> inventoryAssistance(
            @Valid @RequestBody AgentRequest request,
            @AuthenticationPrincipal BookstoreUserPrincipal principal) {
        request.setAgentType("inventory-assistance");
        return ResponseEntity.ok(agentOrchestrator.run(request, principal));
    }

    @PostMapping("/store-support")
    @Operation(summary = "Store Support Agent — bookstore questions and support")
    public ResponseEntity<AgentResponse> storeSupport(
            @Valid @RequestBody AgentRequest request,
            @AuthenticationPrincipal BookstoreUserPrincipal principal) {
        request.setAgentType("store-support");
        return ResponseEntity.ok(agentOrchestrator.run(request, principal));
    }
}
