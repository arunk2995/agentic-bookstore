package com.bookstore.dto.book;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateBookRequest {
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private List<String> authors;

    @Size(max = 255, message = "Publisher name must not exceed 255 characters")
    private String publisher;

    private LocalDate publicationDate;
    private String language;

    @Positive(message = "Pages must be positive")
    private Integer pages;

    @DecimalMin(value = "0.00", message = "Price must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid price format")
    private BigDecimal price;

    private String currency;

    @DecimalMin(value = "0.00", message = "Discount must be non-negative")
    @DecimalMax(value = "100.00", message = "Discount cannot exceed 100%")
    private BigDecimal discountPercent;

    private String coverImageUrl;

    private UUID categoryId;

    private Boolean active;
}
