package com.bookstore.controller;

import com.bookstore.dto.auth.LoginRequest;
import com.bookstore.dto.auth.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Auth Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/auth/register — creates new account and returns tokens")
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser_" + System.currentTimeMillis());
        request.setEmail("testuser_" + System.currentTimeMillis() + "@example.com");
        request.setPassword("TestPass@1234");
        request.setFirstName("Test");
        request.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.user.username").value(request.getUsername()))
            .andExpect(jsonPath("$.user.roles[0]").value("CUSTOMER"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — returns 422 for invalid password")
    void register_invalidPassword_returns422() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("weakpassuser");
        request.setEmail("weakpass@example.com");
        request.setPassword("weak"); // Too short, no uppercase

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — returns 409 for duplicate email")
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest request1 = new RegisterRequest();
        request1.setUsername("user_a_" + System.currentTimeMillis());
        request1.setEmail("duplicate_" + System.currentTimeMillis() + "@test.com");
        request1.setPassword("Password@1");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isCreated());

        RegisterRequest request2 = new RegisterRequest();
        request2.setUsername("user_b_" + System.currentTimeMillis());
        request2.setEmail(request1.getEmail()); // same email
        request2.setPassword("Password@2");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — returns 401 for wrong password")
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("nonexistent@example.com");
        request.setPassword("WrongPass@1");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }
}
