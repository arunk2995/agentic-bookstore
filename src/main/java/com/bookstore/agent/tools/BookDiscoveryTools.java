package com.bookstore.agent.tools;

import com.bookstore.agent.core.*;
import com.bookstore.dto.book.BookSearchRequest;
import com.bookstore.service.BookService;
import com.bookstore.service.InventoryService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tool implementations for the Book Discovery Agent.
 * These delegate to real application services — no direct DB access.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BookDiscoveryTools {

    private final ToolRegistry toolRegistry;
    private final BookService bookService;
    private final InventoryService inventoryService;

    @PostConstruct
    public void registerTools() {
        // searchBooks
        toolRegistry.register(
            ToolDefinition.builder()
                .name("searchBooks")
                .description("Search for books by title, author, category, price range, and language")
                .parameters(Map.of(
                    "query", ToolDefinition.ToolParameter.builder().type("string").description("Search query text").required(false).build(),
                    "category", ToolDefinition.ToolParameter.builder().type("string").description("Category name or slug").required(false).build(),
                    "minPrice", ToolDefinition.ToolParameter.builder().type("number").description("Minimum price").required(false).build(),
                    "maxPrice", ToolDefinition.ToolParameter.builder().type("number").description("Maximum price").required(false).build(),
                    "language", ToolDefinition.ToolParameter.builder().type("string").description("Language code (e.g. en)").required(false).build()
                ))
                .requiredRoles(List.of())
                .requiresAuth(false)
                .build(),
            this::searchBooks
        );

        // getBookDetails
        toolRegistry.register(
            ToolDefinition.builder()
                .name("getBookDetails")
                .description("Get full details for a book by its ID")
                .parameters(Map.of(
                    "bookId", ToolDefinition.ToolParameter.builder().type("string").description("UUID of the book").required(true).build()
                ))
                .requiredRoles(List.of())
                .requiresAuth(false)
                .build(),
            this::getBookDetails
        );

        // getBooksByAuthor
        toolRegistry.register(
            ToolDefinition.builder()
                .name("getBooksByAuthor")
                .description("Find books by a specific author name")
                .parameters(Map.of(
                    "author", ToolDefinition.ToolParameter.builder().type("string").description("Author name").required(true).build()
                ))
                .requiredRoles(List.of())
                .requiresAuth(false)
                .build(),
            this::getBooksByAuthor
        );

        // getRelatedBooks
        toolRegistry.register(
            ToolDefinition.builder()
                .name("getRelatedBooks")
                .description("Get books related to a given book (same category, similar content)")
                .parameters(Map.of(
                    "bookId", ToolDefinition.ToolParameter.builder().type("string").description("UUID of the reference book").required(true).build()
                ))
                .requiredRoles(List.of())
                .requiresAuth(false)
                .build(),
            this::getRelatedBooks
        );

        // getBookAvailability
        toolRegistry.register(
            ToolDefinition.builder()
                .name("getBookAvailability")
                .description("Check current stock availability for a book")
                .parameters(Map.of(
                    "bookId", ToolDefinition.ToolParameter.builder().type("string").description("UUID of the book").required(true).build()
                ))
                .requiredRoles(List.of())
                .requiresAuth(false)
                .build(),
            this::getBookAvailability
        );

        // getTopRatedBooks
        toolRegistry.register(
            ToolDefinition.builder()
                .name("getTopRatedBooks")
                .description("Get highest rated books, optionally filtered by category")
                .parameters(Map.of(
                    "category", ToolDefinition.ToolParameter.builder().type("string").description("Category ID (optional)").required(false).build(),
                    "limit", ToolDefinition.ToolParameter.builder().type("integer").description("Max results").required(false).build()
                ))
                .requiredRoles(List.of())
                .requiresAuth(false)
                .build(),
            this::getTopRatedBooks
        );
    }

    private ToolCallResult searchBooks(ToolCallRequest request) {
        Map<String, Object> args = request.getArguments();
        String query = getString(args, "query");
        BigDecimal minPrice = getBigDecimal(args, "minPrice");
        BigDecimal maxPrice = getBigDecimal(args, "maxPrice");
        String language = getString(args, "language");

        BookSearchRequest searchRequest = BookSearchRequest.builder()
            .query(query)
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .language(language)
            .page(0)
            .size(10)
            .build();

        var page = bookService.searchBooks(searchRequest);
        return ToolCallResult.success(Map.of(
            "totalBooks", page.getTotalElements(),
            "books", page.getContent()
        ));
    }

    private ToolCallResult getBookDetails(ToolCallRequest request) {
        String bookId = getString(request.getArguments(), "bookId");
        if (bookId == null) return ToolCallResult.error("MISSING_PARAM", "bookId is required");
        return ToolCallResult.success(bookService.getBook(UUID.fromString(bookId)));
    }

    private ToolCallResult getBooksByAuthor(ToolCallRequest request) {
        String author = getString(request.getArguments(), "author");
        if (author == null) return ToolCallResult.error("MISSING_PARAM", "author is required");
        var page = bookService.searchBooks(BookSearchRequest.builder().author(author).page(0).size(10).build());
        return ToolCallResult.success(Map.of("author", author, "books", page.getContent()));
    }

    private ToolCallResult getRelatedBooks(ToolCallRequest request) {
        String bookId = getString(request.getArguments(), "bookId");
        if (bookId == null) return ToolCallResult.error("MISSING_PARAM", "bookId is required");
        return ToolCallResult.success(bookService.getRelatedBooks(UUID.fromString(bookId)));
    }

    private ToolCallResult getBookAvailability(ToolCallRequest request) {
        String bookId = getString(request.getArguments(), "bookId");
        if (bookId == null) return ToolCallResult.error("MISSING_PARAM", "bookId is required");
        var inventory = inventoryService.getInventory(UUID.fromString(bookId));
        return ToolCallResult.success(Map.of(
            "bookId", bookId,
            "available", inventory.getQuantityAvailable(),
            "inStock", inventory.getQuantityAvailable() > 0
        ));
    }

    private ToolCallResult getTopRatedBooks(ToolCallRequest request) {
        int limit = getInt(request.getArguments(), "limit", 10);
        var page = bookService.getTopRated(null, limit);
        return ToolCallResult.success(Map.of(
            "books", page.getContent(),
            "count", page.getTotalElements()
        ));
    }

    // Helpers
    private String getString(Map<String, Object> args, String key) {
        if (args == null) return null;
        Object val = args.get(key);
        return val != null ? val.toString() : null;
    }

    private BigDecimal getBigDecimal(Map<String, Object> args, String key) {
        if (args == null) return null;
        Object val = args.get(key);
        if (val == null) return null;
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return null; }
    }

    private int getInt(Map<String, Object> args, String key, int defaultVal) {
        if (args == null) return defaultVal;
        Object val = args.get(key);
        if (val == null) return defaultVal;
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return defaultVal; }
    }
}
