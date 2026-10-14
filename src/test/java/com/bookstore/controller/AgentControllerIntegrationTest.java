package com.bookstore.controller;

import com.bookstore.agent.core.AgentRequest;
import com.bookstore.dto.auth.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Agent Integration Tests")
class AgentControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String customerToken;

    @BeforeEach
    void setup() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("agenttest_" + System.currentTimeMillis());
        reg.setEmail("agenttest_" + System.currentTimeMillis() + "@example.com");
        reg.setPassword("TestPass@1234");

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
            .andReturn();

        JsonNode body = objectMapper.readTree(regResult.getResponse().getContentAsString());
        customerToken = body.get("accessToken").asText();
    }

    @Test
    @DisplayName("POST /api/v1/agents/book-discovery — returns agent response (no auth needed)")
    void bookDiscovery_noAuth_succeeds() throws Exception {
        AgentRequest request = new AgentRequest();
        request.setMessage("Find Java programming books under ₹1000");

        mockMvc.perform(post("/api/v1/agents/book-discovery")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correlationId").isNotEmpty())
            .andExpect(jsonPath("$.message").isNotEmpty())
            .andExpect(jsonPath("$.agentType").value("book-discovery"));
    }

    @Test
    @DisplayName("POST /api/v1/agents/store-support — returns policy information")
    void storeSupport_shippingQuery_returnsPolicyInfo() throws Exception {
        AgentRequest request = new AgentRequest();
        request.setMessage("What is your shipping policy?");

        mockMvc.perform(post("/api/v1/agents/store-support")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/agents/inventory-assistance — returns 403 for CUSTOMER")
    void inventoryAssistance_asCustomer_returns403() throws Exception {
        AgentRequest request = new AgentRequest();
        request.setMessage("Show me low stock books");

        mockMvc.perform(post("/api/v1/agents/inventory-assistance")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/agents/book-discovery — handles large message gracefully")
    void bookDiscovery_withinSizeLimit_succeeds() throws Exception {
        AgentRequest request = new AgentRequest();
        request.setMessage("I am looking for books on advanced topics in distributed systems and microservices architecture");

        mockMvc.perform(post("/api/v1/agents/book-discovery")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }
}
