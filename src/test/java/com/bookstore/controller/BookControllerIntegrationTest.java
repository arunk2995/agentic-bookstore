package com.bookstore.controller;

import com.bookstore.dto.auth.LoginRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Book API Integration Tests")
class BookControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String customerToken;

    @BeforeEach
    void setup() throws Exception {
        // Register and login a customer
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("booktest_" + System.currentTimeMillis());
        reg.setEmail("booktest_" + System.currentTimeMillis() + "@example.com");
        reg.setPassword("TestPass@1234");

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
            .andReturn();

        JsonNode body = objectMapper.readTree(regResult.getResponse().getContentAsString());
        customerToken = body.get("accessToken").asText();
    }

    @Test
    @DisplayName("GET /api/v1/books — returns paginated book list without auth")
    void getBooks_noAuth_returnsBooks() throws Exception {
        mockMvc.perform(get("/api/v1/books"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    @DisplayName("GET /api/v1/books?query=java — filters by query")
    void getBooks_withQuery_filtersResults() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("query", "java"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/books — returns 403 for CUSTOMER role")
    void createBook_asCustomer_returns403() throws Exception {
        String body = """
            {
              "isbn": "978-1234567890",
              "title": "Test Book",
              "authors": ["Author One"],
              "price": 499.00
            }
            """;

        mockMvc.perform(post("/api/v1/books")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/books/{id} — returns 404 for unknown book ID")
    void getBook_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/books/00000000-0000-0000-0000-000000000999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/books/top-rated — returns top rated books")
    void getTopRated_returnsBooks() throws Exception {
        mockMvc.perform(get("/api/v1/books/top-rated").param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }
}
