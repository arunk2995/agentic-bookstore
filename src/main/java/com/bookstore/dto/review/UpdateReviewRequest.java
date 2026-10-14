package com.bookstore.dto.review;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateReviewRequest {
    @Min(1) @Max(5)
    private Integer rating;

    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String content;
}
