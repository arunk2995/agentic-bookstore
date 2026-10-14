package com.bookstore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Agentic eCommerce Bookstore API")
                .description("""
                    Production-oriented Spring Boot backend for an AI-powered bookstore.
                    
                    **Features:**
                    - Complete book catalog with search, filtering, and pagination
                    - Shopping cart and secure checkout
                    - Order management with inventory reservation
                    - 5 AI agents powered by a tool-calling framework
                    - Role-based access control (CUSTOMER, ADMIN, INVENTORY_MANAGER)
                    
                    **Test credentials (from seed data):**
                    - Admin: `admin@bookstore.example.com` / `Admin@1234`
                    - Customer: `customer1@example.com` / `Admin@1234`
                    - Inventory Manager: `invmgr@bookstore.example.com` / `Admin@1234`
                    """)
                .version("1.0.0")
                .contact(new Contact().name("Bookstore Team").email("api@bookstore.example.com")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .name("bearerAuth")
                    .description("JWT token from /api/v1/auth/login")));
    }
}
