package com.bookstore.dto.review;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateReviewRequest {
    @Min(1) @Max(5)
    private int rating;

    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String content;
}
