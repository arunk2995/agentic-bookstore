package com.bookstore.dto.book;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateBookRequest {
    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "^(978|979)[0-9]{10}$|^[0-9]{10}$", message = "Invalid ISBN format")
    private String isbn;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotEmpty(message = "At least one author is required")
    private List<String> authors;

    @Size(max = 255, message = "Publisher name must not exceed 255 characters")
    private String publisher;

    private LocalDate publicationDate;

    @Size(min = 2, max = 10, message = "Language code must be 2-10 characters")
    private String language;

    @Positive(message = "Pages must be positive")
    private Integer pages;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid price format")
    private BigDecimal price;

    @Size(min = 3, max = 3, message = "Currency must be a 3-character code")
    private String currency;

    @DecimalMin(value = "0.00", message = "Discount must be non-negative")
    @DecimalMax(value = "100.00", message = "Discount cannot exceed 100%")
    private BigDecimal discountPercent;

    private String coverImageUrl;

    private UUID categoryId;

    @Min(value = 0, message = "Initial inventory must be non-negative")
    private int initialInventory;
}
