package com.bookstore.dto.review;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ReviewResponse {
    private UUID id;
    private UUID bookId;
    private UUID userId;
    private String reviewerName;
    private int rating;
    private String title;
    private String content;
    private boolean approved;
    private Instant createdAt;
    private Instant updatedAt;
}
