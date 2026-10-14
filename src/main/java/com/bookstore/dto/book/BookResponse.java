package com.bookstore.dto.book;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BookResponse {
    private UUID id;
    private String isbn;
    private String title;
    private String description;
    private List<String> authors;
    private String publisher;
    private LocalDate publicationDate;
    private String language;
    private Integer pages;
    private BigDecimal price;
    private String currency;
    private BigDecimal discountPercent;
    private BigDecimal effectivePrice;
    private String coverImageUrl;
    private BigDecimal averageRating;
    private int reviewCount;
    private boolean active;
    private CategoryInfo category;
    private int availableQuantity;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    public static class CategoryInfo {
        private UUID id;
        private String name;
        private String slug;
    }
}
