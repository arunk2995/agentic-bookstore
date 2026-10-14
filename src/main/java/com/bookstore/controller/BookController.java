package com.bookstore.controller;

import com.bookstore.dto.book.*;
import com.bookstore.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Book catalog management")
public class BookController {

    private final BookService bookService;

    @GetMapping
    @Operation(summary = "Search and browse books")
    public ResponseEntity<Page<BookResponse>> searchBooks(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {

        BookSearchRequest request = BookSearchRequest.builder()
            .query(query)
            .categoryId(categoryId)
            .author(author)
            .publisher(publisher)
            .language(language)
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .page(page)
            .size(size)
            .sortBy(sortBy)
            .sortDir(sortDir)
            .build();

        return ResponseEntity.ok(bookService.searchBooks(request));
    }

    @GetMapping("/{bookId}")
    @Operation(summary = "Get book details by ID")
    public ResponseEntity<BookResponse> getBook(@PathVariable UUID bookId) {
        return ResponseEntity.ok(bookService.getBook(bookId));
    }

    @GetMapping("/isbn/{isbn}")
    @Operation(summary = "Get book by ISBN")
    public ResponseEntity<BookResponse> getBookByIsbn(@PathVariable String isbn) {
        return ResponseEntity.ok(bookService.getBookByIsbn(isbn));
    }

    @GetMapping("/{bookId}/related")
    @Operation(summary = "Get related books")
    public ResponseEntity<List<BookResponse>> getRelatedBooks(@PathVariable UUID bookId) {
        return ResponseEntity.ok(bookService.getRelatedBooks(bookId));
    }

    @GetMapping("/top-rated")
    @Operation(summary = "Get top-rated books")
    public ResponseEntity<Page<BookResponse>> getTopRated(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(bookService.getTopRated(categoryId, limit));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a new book (Admin only)")
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBook(request));
    }

    @PutMapping("/{bookId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a book (Admin only)")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable UUID bookId,
            @Valid @RequestBody UpdateBookRequest request) {
        return ResponseEntity.ok(bookService.updateBook(bookId, request));
    }

    @DeleteMapping("/{bookId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Deactivate a book (Admin only)")
    public ResponseEntity<Void> deactivateBook(@PathVariable UUID bookId) {
        bookService.deactivateBook(bookId);
        return ResponseEntity.noContent().build();
    }
}
