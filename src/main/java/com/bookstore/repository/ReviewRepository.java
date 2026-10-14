package com.bookstore.repository;

import com.bookstore.domain.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsByBookIdAndUserId(UUID bookId, UUID userId);
    Optional<Review> findByBookIdAndUserId(UUID bookId, UUID userId);
    Page<Review> findByBookIdAndApprovedTrueOrderByCreatedAtDesc(UUID bookId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId AND r.approved = true")
    Double calculateAverageRating(UUID bookId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.book.id = :bookId AND r.approved = true")
    long countApprovedByBookId(UUID bookId);
}
