# Agentic eCommerce Bookstore

A production-oriented **Spring Boot 3.x** backend for an online bookstore with a full **agentic AI layer** supporting tool-calling LLM agents. Built with Java 21, PostgreSQL, JWT authentication, and five specialized AI agents.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)  
2. [Quick Start with Docker](#2-quick-start-with-docker)  
3. [Local Development Setup](#3-local-development-setup)  
4. [Configuration](#4-configuration)  
5. [API Reference](#5-api-reference)  
6. [Agentic AI Layer](#6-agentic-ai-layer)  
7. [Authentication](#7-authentication)  
8. [Running Tests](#8-running-tests)  
9. [Database Migrations](#9-database-migrations)  
10. [Security Notes](#10-security-notes)  
11. [Known Limitations & Future Work](#11-known-limitations--future-work)  

---

## 1. Architecture Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                      REST API (Spring Boot)                       │
│  /api/v1/auth  /books  /cart  /orders  /inventory  /agents       │
└────────────────────────────┬─────────────────────────────────────┘
                             │
     ┌───────────────────────┴──────────────────────────┐
     │              Application Services                  │
     │  AuthService  BookService  CartService             │
     │  OrderService  InventoryService  ReviewService     │
     └──────────────────────┬───────────────────────────┘
                            │
     ┌──────────────────────┴───────────────────────────┐
     │           Agentic Layer (Tool-calling)            │
     │                                                    │
     │  AgentOrchestrator                                 │
     │    ├── LlmProvider (mock/OpenAI)                   │
     │    ├── ToolRegistry (searchBooks, getInventory…)   │
     │    └── ToolExecutionService (auth + audit)         │
     └──────────────────────┬───────────────────────────┘
                            │
     ┌──────────────────────┴───────────────────────────┐
     │                 Infrastructure                     │
     │  PostgreSQL (JPA/Hibernate)  Flyway               │
     │  JWT (JJWT)  BCrypt  Spring Security              │
     └───────────────────────────────────────────────────┘
```

### Package Structure

```
com.bookstore
├── agent/
│   ├── core/          # AgentOrchestrator, ToolRegistry, ToolExecutionService
│   ├── llm/           # LlmProvider, MockLlmProvider
│   └── tools/         # BookDiscoveryTools, InventoryTools, OrderAssistanceTools, StoreSupportTools
├── config/            # SecurityConfig, OpenApiConfig, BookstoreProperties
├── controller/        # REST controllers (Auth, Book, Cart, Order, Inventory, Review, Agent)
├── domain/            # JPA entities (User, Book, Order, Inventory, Review, Agent…)
├── dto/               # Request/Response DTOs per domain
├── exception/         # GlobalExceptionHandler, BookstoreException
├── integration/       # PaymentService, EmailService, ShippingService (+ mock impls)
├── repository/        # Spring Data JPA repositories
├── security/          # JwtTokenProvider, JwtAuthenticationFilter, BookstoreUserPrincipal
└── service/           # Application services
```

---

## 2. Quick Start with Docker

**Prerequisites:** Docker Desktop 24+, Docker Compose V2

```bash
# 1. Clone the repository
git clone <repo-url>
cd agentic-bookstore

# 2. Configure environment
cp .env.example .env
# Edit .env if needed (defaults work for local development)

# 3. Start the full stack
docker compose up --build

# 4. Application is ready at http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

The first startup will:
- Start PostgreSQL
- Run Flyway migrations (V1 schema + V2 seed data)
- Create sample books, categories, users, and inventory
- Start the application with all mock integrations enabled

---

## 3. Local Development Setup

**Prerequisites:** Java 21, Maven 3.9+, PostgreSQL 16+

```bash
# 1. Create the database
psql -U postgres -c "CREATE DATABASE bookstore;"
psql -U postgres -c "CREATE USER bookstore WITH PASSWORD 'bookstore123';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE bookstore TO bookstore;"

# 2. Set environment variables (or add to your IDE run config)
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bookstore
export SPRING_DATASOURCE_USERNAME=bookstore
export SPRING_DATASOURCE_PASSWORD=bookstore123
export JWT_SECRET=bookstore-jwt-secret-key-must-be-at-least-256-bits-long

# 3. Build and run
mvn spring-boot:run

# OR build JAR and run
mvn package -DskipTests
java -jar target/agentic-bookstore-1.0.0-SNAPSHOT.jar
```

---

## 4. Configuration

All configuration is in `src/main/resources/application.yml`. Secrets are supplied via environment variables.

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | jdbc:postgresql://localhost:5432/bookstore | Database URL |
| `SPRING_DATASOURCE_USERNAME` | bookstore | DB username |
| `SPRING_DATASOURCE_PASSWORD` | bookstore123 | DB password |
| `JWT_SECRET` | _(change in prod)_ | JWT signing secret (≥256 bits) |
| `JWT_ACCESS_TOKEN_EXPIRY` | 3600 | Access token expiry (seconds) |
| `AGENT_MOCK_MODE` | true | Use mock LLM (no API key needed) |
| `OPENAI_API_KEY` | mock-key | Real OpenAI key (when mock mode off) |
| `PAYMENT_MOCK_MODE` | true | Use mock payment provider |
| `EMAIL_MOCK_MODE` | true | Use mock email service |

### Enabling Real AI

To use a real OpenAI-compatible LLM:

```bash
export AGENT_MOCK_MODE=false
export OPENAI_API_KEY=sk-your-key-here
export OPENAI_MODEL=gpt-4o-mini
```

---

## 5. API Reference

**Base URL:** `http://localhost:8080`  
**Swagger UI:** `http://localhost:8080/swagger-ui.html`  
**OpenAPI JSON:** `http://localhost:8080/api-docs`

### Authentication

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"Alice@1234","firstName":"Alice"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"alice@example.com","password":"Alice@1234"}'
# → Returns {"accessToken":"...", "refreshToken":"..."}
```

### Books

```bash
# Browse all books
curl http://localhost:8080/api/v1/books

# Search with filters
curl "http://localhost:8080/api/v1/books?query=java&maxPrice=1000&sortBy=rating"

# Get a specific book
curl http://localhost:8080/api/v1/books/{bookId}
```

### Shopping Cart

```bash
TOKEN="Bearer <your-access-token>"

# View cart
curl -H "Authorization: $TOKEN" http://localhost:8080/api/v1/cart

# Add item
curl -X POST -H "Authorization: $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"bookId":"<uuid>","quantity":2}' \
  http://localhost:8080/api/v1/cart/items
```

### Orders

```bash
# Place an order (requires items in cart)
curl -X POST -H "Authorization: $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"shippingAddressLine1":"123 Main St","shippingCity":"Mumbai","shippingCountry":"IN","paymentToken":"test-token"}' \
  http://localhost:8080/api/v1/orders

# View my orders
curl -H "Authorization: $TOKEN" http://localhost:8080/api/v1/orders
```

### Seed Test Users

| Role | Email | Password |
|---|---|---|
| ADMIN | admin@bookstore.example.com | Admin@1234 |
| CUSTOMER | customer1@example.com | Admin@1234 |
| INVENTORY_MANAGER | invmgr@bookstore.example.com | Admin@1234 |

---

## 6. Agentic AI Layer

Five specialized agents are available via `POST /api/v1/agents/{agent-name}`.

All agents follow the same request format:

```json
{
  "message": "Find Java books under ₹1000",
  "correlationId": "optional-correlation-id"
}
```

### Agents

| Endpoint | Agent | Auth Required | Description |
|---|---|---|---|
| `/agents/book-discovery` | Book Discovery | No | Search catalog using natural language |
| `/agents/recommendations` | Recommendations | Yes (CUSTOMER) | Personalized book recommendations |
| `/agents/order-assistance` | Order Assistance | Yes (CUSTOMER) | Help with orders, cancellations |
| `/agents/inventory-assistance` | Inventory Ops | Yes (ADMIN/INV_MGR) | Inventory analysis and proposals |
| `/agents/store-support` | Store Support | No | Policies, general questions |

### Example: Book Discovery

```bash
curl -X POST http://localhost:8080/api/v1/agents/book-discovery \
  -H "Content-Type: application/json" \
  -d '{"message": "Find beginner-friendly Java books under 1000 rupees"}'
```

**Response:**
```json
{
  "correlationId": "abc-123",
  "agentType": "book-discovery",
  "message": "Here are the books I found based on your query...",
  "toolCallsCount": 1,
  "durationMs": 42
}
```

### Tool Execution Flow

```
User request → Authentication → AgentOrchestrator
  → LlmProvider.chat() → [TOOL_CALLS response]
    → ToolExecutionService.execute()
      → Authorization check
      → Argument validation
      → Application service (e.g., BookService.searchBooks())
      → Audit log
    → Results fed back to LLM
  → LlmProvider.chat() → [TEXT response]
→ API response
```

### Mock LLM Mode

When `AGENT_MOCK_MODE=true` (default):
- No external API key required
- The mock LLM detects keywords in the user message and selects appropriate tools
- Tools still call **real application services** with validated arguments against the real database
- Responses are deterministic and repeatable for testing

---

## 7. Authentication

JWT-based bearer tokens. Include in requests:

```
Authorization: Bearer <access-token>
```

Token expiry is configurable (`JWT_ACCESS_TOKEN_EXPIRY`, default 1 hour). Use `POST /api/v1/auth/refresh` with the refresh token to get a new access token.

### Roles

| Role | Capabilities |
|---|---|
| `CUSTOMER` | Browse, cart, orders, reviews, agent endpoints |
| `ADMIN` | Full access including book management, admin APIs |
| `INVENTORY_MANAGER` | Inventory management, inventory agent |

---

## 8. Running Tests

```bash
# Run all tests (uses H2 in-memory DB — no PostgreSQL needed)
mvn test

# Run only unit tests
mvn test -Dtest="*Test"

# Run only integration tests  
mvn test -Dtest="*IntegrationTest"

# Run with verbose output
mvn test -Dsurefire.useFile=false
```

### Test Categories

| Test Class | Type | Coverage |
|---|---|---|
| `BookServiceTest` | Unit | Search, catalog operations, discount calc |
| `InventoryServiceTest` | Unit | Reservation, release, low stock |
| `OrderStatusTest` | Unit | State machine transitions |
| `JwtTokenProviderTest` | Unit | JWT generation, validation, extraction |
| `AgentOrchestratorTest` | Unit | Tool call routing, error handling |
| `AuthControllerIntegrationTest` | Integration | Register, login, validation |
| `BookControllerIntegrationTest` | Integration | CRUD, search, authorization |
| `AgentControllerIntegrationTest` | Integration | Agent endpoints, auth, mock LLM |
| `BookstoreApplicationTests` | Integration | Spring context loads |

---

## 9. Database Migrations

Flyway migrations run automatically on startup. Scripts in `src/main/resources/db/migration/`:

| Migration | Description |
|---|---|
| `V1__initial_schema.sql` | Complete schema: all tables, indexes, constraints |
| `V2__seed_data.sql` | Sample data: 10 books, 3 users, categories, inventory |

For manual Flyway operations:

```bash
# Run migrations manually (requires env vars set)
mvn flyway:migrate

# Check migration status
mvn flyway:info
```

---

## 10. Security Notes

- **Passwords**: BCrypt with configurable strength (default 12 rounds)
- **JWT**: HS256 signed with a configurable secret — **change `JWT_SECRET` in production**
- **SQL Injection**: All DB access via JPA with parameterized queries
- **Authorization**: Resource-level checks (users can only access their own orders/cart)
- **Agent Security**: User identity derived from Spring Security context — LLM cannot impersonate users
- **Payment**: Mock-only in current implementation; real integration requires webhook signature verification
- **Secrets**: Never committed — always supplied via environment variables

### Production Checklist

- [ ] Change `JWT_SECRET` to a 256-bit random value
- [ ] Change `POSTGRES_PASSWORD` to a strong password
- [ ] Set `SPRING_JPA_SHOW_SQL=false`
- [ ] Enable HTTPS via a reverse proxy (nginx/ALB)
- [ ] Set `AGENT_MOCK_MODE=false` and configure `OPENAI_API_KEY`
- [ ] Configure real payment, email, and shipping providers
- [ ] Enable Spring Security HTTPS-only cookies if using session fallback

---

## 11. Known Limitations & Future Work

### Current Limitations

1. **Spring AI dependency**: The `spring-ai-openai-spring-boot-starter` is a milestone release. For real AI integration, pin to a stable GA release when available. The mock LLM works fully without it.
2. **No rate limiting middleware**: Rate limiting is configured but uses an in-process counter (Bucket4j). For distributed deployments, use Redis-backed Bucket4j.
3. **No real payment provider**: Payment, email, and shipping are mocked. Production integration requires real provider implementation.
4. **No refresh token rotation**: Refresh tokens are stateless JWTs. Production hardening should add refresh token rotation with a revocation table.
5. **No conversation history**: The agent doesn't persist multi-turn conversation state across requests. Each request starts fresh.
6. **H2 for tests**: Integration tests use H2 with PostgreSQL-compatibility mode. A Testcontainers-based test suite with real PostgreSQL would provide higher fidelity.

### Future Extension Points

- **Redis caching**: Book catalog and inventory responses are suitable for Redis caching
- **Elasticsearch**: Full-text search integration for better catalog search
- **Event-driven**: Order events via Kafka/RabbitMQ for async fulfillment
- **Wishlist & recommendations**: ML-based personalization engine
- **Multi-currency**: Currency conversion service integration
- **Admin dashboard**: React/Angular frontend for admin operations
- **Kubernetes**: Helm chart for cloud deployment
