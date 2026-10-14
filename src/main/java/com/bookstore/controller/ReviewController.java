package com.bookstore.controller;

import com.bookstore.dto.review.*;
import com.bookstore.security.BookstoreUserPrincipal;
import com.bookstore.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/books/{bookId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Book review management")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Get approved reviews for a book")
    public ResponseEntity<Page<ReviewResponse>> getReviews(
            @PathVariable UUID bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewService.getBookReviews(bookId, page, size));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Submit a review for a book")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable UUID bookId,
            @AuthenticationPrincipal BookstoreUserPrincipal principal,
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(reviewService.createReview(bookId, principal.getUserId(), request));
    }

    @PutMapping("/{reviewId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update your review")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable UUID bookId,
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal BookstoreUserPrincipal principal,
            @Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(reviewService.updateReview(bookId, reviewId, principal.getUserId(), request));
    }

    @DeleteMapping("/{reviewId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete your review")
    public ResponseEntity<Void> deleteReview(
            @PathVariable UUID bookId,
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal BookstoreUserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        reviewService.deleteReview(bookId, reviewId, principal.getUserId(), isAdmin);
        return ResponseEntity.noContent().build();
    }
}
