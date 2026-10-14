package com.bookstore.service;

import com.bookstore.domain.catalog.Book;
import com.bookstore.domain.review.Review;
import com.bookstore.domain.user.User;
import com.bookstore.dto.review.*;
import com.bookstore.exception.BookstoreException;
import com.bookstore.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse createReview(UUID bookId, UUID userId, CreateReviewRequest request) {
        if (reviewRepository.existsByBookIdAndUserId(bookId, userId)) {
            throw BookstoreException.conflict("You have already reviewed this book");
        }

        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> BookstoreException.notFound("Book", bookId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> BookstoreException.notFound("User", userId));

        Review review = Review.builder()
            .book(book)
            .user(user)
            .rating(request.getRating())
            .title(request.getTitle())
            .content(request.getContent())
            .approved(false) // requires moderation
            .build();

        review = reviewRepository.save(review);
        log.info("Review created for book {} by user {}", bookId, userId);
        return toResponse(review);
    }

    @Transactional
    public ReviewResponse updateReview(UUID bookId, UUID reviewId, UUID userId, UpdateReviewRequest request) {
        Review review = reviewRepository.findByBookIdAndUserId(bookId, userId)
            .orElseThrow(() -> BookstoreException.notFound("Review", reviewId));

        if (!review.getId().equals(reviewId)) {
            throw BookstoreException.forbidden("You can only edit your own reviews");
        }

        if (request.getRating() != null) review.setRating(request.getRating());
        if (request.getTitle() != null) review.setTitle(request.getTitle());
        if (request.getContent() != null) review.setContent(request.getContent());
        review.setApproved(false); // re-review after edit

        review = reviewRepository.save(review);
        return toResponse(review);
    }

    @Transactional
    public void deleteReview(UUID bookId, UUID reviewId, UUID userId, boolean isAdmin) {
        Review review;
        if (isAdmin) {
            review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> BookstoreException.notFound("Review", reviewId));
        } else {
            review = reviewRepository.findByBookIdAndUserId(bookId, userId)
                .orElseThrow(() -> BookstoreException.notFound("Review", reviewId));
            if (!review.getId().equals(reviewId)) {
                throw BookstoreException.forbidden("You can only delete your own reviews");
            }
        }

        reviewRepository.delete(review);
        recalculateAverageRating(bookId);
    }

    @Transactional
    public ReviewResponse approveReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> BookstoreException.notFound("Review", reviewId));
        review.setApproved(true);
        review.setApprovedAt(java.time.Instant.now());
        review = reviewRepository.save(review);
        recalculateAverageRating(review.getBook().getId());
        return toResponse(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getBookReviews(UUID bookId, int page, int size) {
        if (!bookRepository.existsById(bookId)) {
            throw BookstoreException.notFound("Book", bookId);
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return reviewRepository.findByBookIdAndApprovedTrueOrderByCreatedAtDesc(bookId, pageable)
            .map(this::toResponse);
    }

    private void recalculateAverageRating(UUID bookId) {
        Double avg = reviewRepository.calculateAverageRating(bookId);
        long count = reviewRepository.countApprovedByBookId(bookId);

        bookRepository.findById(bookId).ifPresent(book -> {
            book.setAverageRating(avg != null ?
                BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            book.setReviewCount((int) count);
            bookRepository.save(book);
        });
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
            .id(review.getId())
            .bookId(review.getBook().getId())
            .userId(review.getUser().getId())
            .reviewerName(review.getUser().getFirstName() != null ?
                review.getUser().getFirstName() + " " + (review.getUser().getLastName() != null ?
                    review.getUser().getLastName() : "") :
                review.getUser().getUsername())
            .rating(review.getRating())
            .title(review.getTitle())
            .content(review.getContent())
            .approved(review.isApproved())
            .createdAt(review.getCreatedAt())
            .updatedAt(review.getUpdatedAt())
            .build();
    }
}
