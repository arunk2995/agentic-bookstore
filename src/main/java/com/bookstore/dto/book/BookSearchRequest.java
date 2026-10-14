package com.bookstore.dto.book;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BookSearchRequest {
    private String query;
    private UUID categoryId;
    private String author;
    private String publisher;
    private String language;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean available;
    private int page;
    private int size;
    private String sortBy;
    private String sortDir;
}
