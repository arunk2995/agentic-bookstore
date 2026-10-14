package com.bookstore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bookstore")
@Data
public class BookstoreProperties {

    private Jwt jwt = new Jwt();
    private Security security = new Security();
    private Pagination pagination = new Pagination();
    private Inventory inventory = new Inventory();
    private Agent agent = new Agent();
    private Payment payment = new Payment();
    private Email email = new Email();
    private Shipping shipping = new Shipping();

    @Data
    public static class Jwt {
        private String secret;
        private long accessTokenExpiry = 3600;
        private long refreshTokenExpiry = 86400;
        private String issuer = "agentic-bookstore";
    }

    @Data
    public static class Security {
        private int bcryptStrength = 12;
        private int passwordResetTokenExpiry = 3600;
        private RateLimit rateLimit = new RateLimit();

        @Data
        public static class RateLimit {
            private int loginAttempts = 10;
            private int loginWindowSeconds = 60;
            private int agentRequestsPerMinute = 20;
        }
    }

    @Data
    public static class Pagination {
        private int defaultPageSize = 20;
        private int maxPageSize = 100;
    }

    @Data
    public static class Inventory {
        private int lowStockThreshold = 10;
        private int reservationExpiryMinutes = 30;
    }

    @Data
    public static class Agent {
        private int maxIterations = 10;
        private int timeoutSeconds = 60;
        private int maxTokens = 4096;
        private boolean mockMode = true;
        private OpenAI openai = new OpenAI();

        @Data
        public static class OpenAI {
            private String apiKey = "mock-key";
            private String model = "gpt-4o-mini";
            private String baseUrl = "https://api.openai.com";
        }
    }

    @Data
    public static class Payment {
        private boolean mockMode = true;
        private String providerUrl;
        private String webhookSecret;
    }

    @Data
    public static class Email {
        private boolean mockMode = true;
        private String fromAddress;
        private String fromName;
    }

    @Data
    public static class Shipping {
        private boolean mockMode = true;
        private String providerUrl;
    }
}
