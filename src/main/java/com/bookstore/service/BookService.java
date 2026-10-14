package com.bookstore.service;

import com.bookstore.domain.catalog.Author;
import com.bookstore.domain.catalog.Book;
import com.bookstore.domain.catalog.Category;
import com.bookstore.domain.inventory.Inventory;
import com.bookstore.dto.book.*;
import com.bookstore.exception.BookstoreException;
import com.bookstore.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public BookResponse createBook(CreateBookRequest request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw BookstoreException.conflict("Book with ISBN already exists: " + request.getIsbn());
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> BookstoreException.notFound("Category", request.getCategoryId()));
        }

        Book book = Book.builder()
            .isbn(request.getIsbn())
            .title(request.getTitle())
            .description(request.getDescription())
            .publisher(request.getPublisher())
            .publicationDate(request.getPublicationDate())
            .language(request.getLanguage() != null ? request.getLanguage() : "en")
            .pages(request.getPages())
            .price(request.getPrice())
            .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
            .discountPercent(request.getDiscountPercent() != null ? request.getDiscountPercent() : BigDecimal.ZERO)
            .coverImageUrl(request.getCoverImageUrl())
            .category(category)
            .build();

        // Resolve or create authors
        for (String authorName : request.getAuthors()) {
            Author author = authorRepository.findByNameIgnoreCase(authorName)
                .orElseGet(() -> authorRepository.save(Author.builder().name(authorName).build()));
            book.addAuthor(author);
        }

        book = bookRepository.save(book);

        // Create inventory record
        Inventory inventory = Inventory.builder()
            .book(book)
            .quantityOnHand(request.getInitialInventory())
            .quantityAvailable(request.getInitialInventory())
            .quantityReserved(0)
            .build();
        inventoryRepository.save(inventory);

        log.info("Created book: {} (ISBN: {})", book.getTitle(), book.getIsbn());
        return toResponse(book, request.getInitialInventory());
    }

    @Transactional
    public BookResponse updateBook(UUID bookId, UpdateBookRequest request) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> BookstoreException.notFound("Book", bookId));

        if (request.getIsbn() != null &&
            bookRepository.existsByIsbnAndIdNot(request.getIsbn(), bookId)) {
            throw BookstoreException.conflict("ISBN already in use: " + request.getIsbn());
        }

        if (request.getTitle() != null) book.setTitle(request.getTitle());
        if (request.getDescription() != null) book.setDescription(request.getDescription());
        if (request.getPublisher() != null) book.setPublisher(request.getPublisher());
        if (request.getPublicationDate() != null) book.setPublicationDate(request.getPublicationDate());
        if (request.getLanguage() != null) book.setLanguage(request.getLanguage());
        if (request.getPages() != null) book.setPages(request.getPages());
        if (request.getPrice() != null) book.setPrice(request.getPrice());
        if (request.getCurrency() != null) book.setCurrency(request.getCurrency());
        if (request.getDiscountPercent() != null) book.setDiscountPercent(request.getDiscountPercent());
        if (request.getCoverImageUrl() != null) book.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getActive() != null) book.setActive(request.getActive());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> BookstoreException.notFound("Category", request.getCategoryId()));
            book.setCategory(category);
        }

        if (request.getAuthors() != null && !request.getAuthors().isEmpty()) {
            book.getAuthors().clear();
            for (String authorName : request.getAuthors()) {
                Author author = authorRepository.findByNameIgnoreCase(authorName)
                    .orElseGet(() -> authorRepository.save(Author.builder().name(authorName).build()));
                book.addAuthor(author);
            }
        }

        book = bookRepository.save(book);
        int available = getAvailableQuantity(bookId);
        return toResponse(book, available);
    }

    @Transactional(readOnly = true)
    public BookResponse getBook(UUID bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> BookstoreException.notFound("Book", bookId));
        int available = getAvailableQuantity(bookId);
        return toResponse(book, available);
    }

    @Transactional(readOnly = true)
    public BookResponse getBookByIsbn(String isbn) {
        Book book = bookRepository.findByIsbn(isbn)
            .orElseThrow(() -> BookstoreException.notFound("Book", isbn));
        int available = getAvailableQuantity(book.getId());
        return toResponse(book, available);
    }

    @Transactional(readOnly = true)
    public Page<BookResponse> searchBooks(BookSearchRequest request) {
        int size = Math.min(request.getSize() > 0 ? request.getSize() : 20, 100);
        Sort sort = buildSort(request.getSortBy(), request.getSortDir());
        Pageable pageable = PageRequest.of(Math.max(request.getPage(), 0), size, sort);

        Specification<Book> spec = buildSearchSpec(request);
        Page<Book> books = bookRepository.findAll(spec, pageable);

        return books.map(b -> toResponse(b, getAvailableQuantity(b.getId())));
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getRelatedBooks(UUID bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> BookstoreException.notFound("Book", bookId));
        if (book.getCategory() == null) return List.of();
        List<Book> related = bookRepository.findRelatedBooks(bookId, book.getCategory().getId(),
            PageRequest.of(0, 5));
        return related.stream()
            .map(b -> toResponse(b, getAvailableQuantity(b.getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<BookResponse> getTopRated(UUID categoryId, int limit) {
        Pageable pageable = PageRequest.of(0, Math.min(limit, 50));
        return bookRepository.findTopRated(categoryId, pageable)
            .map(b -> toResponse(b, getAvailableQuantity(b.getId())));
    }

    @Transactional
    public void deactivateBook(UUID bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> BookstoreException.notFound("Book", bookId));
        book.setActive(false);
        bookRepository.save(book);
        log.info("Deactivated book: {}", bookId);
    }

    private int getAvailableQuantity(UUID bookId) {
        return inventoryRepository.findByBookId(bookId)
            .map(Inventory::getQuantityAvailable)
            .orElse(0);
    }

    private Specification<Book> buildSearchSpec(BookSearchRequest request) {
        List<Specification<Book>> specs = new ArrayList<>();

        // Text search
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            String pattern = "%" + request.getQuery().toLowerCase() + "%";
            specs.add((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("isbn")), pattern),
                cb.like(cb.lower(root.get("publisher")), pattern)
            ));
        }

        // Category filter
        if (request.getCategoryId() != null) {
            specs.add((root, query, cb) ->
                cb.equal(root.get("category").get("id"), request.getCategoryId()));
        }

        // Author filter
        if (request.getAuthor() != null && !request.getAuthor().isBlank()) {
            String pattern = "%" + request.getAuthor().toLowerCase() + "%";
            specs.add((root, query, cb) -> {
                Join<Object, Object> authors = root.join("authors");
                return cb.like(cb.lower(authors.get("name")), pattern);
            });
        }

        // Publisher filter
        if (request.getPublisher() != null && !request.getPublisher().isBlank()) {
            String pattern = "%" + request.getPublisher().toLowerCase() + "%";
            specs.add((root, query, cb) ->
                cb.like(cb.lower(root.get("publisher")), pattern));
        }

        // Language filter
        if (request.getLanguage() != null && !request.getLanguage().isBlank()) {
            specs.add((root, query, cb) ->
                cb.equal(cb.lower(root.get("language")), request.getLanguage().toLowerCase()));
        }

        // Price range
        if (request.getMinPrice() != null) {
            specs.add((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("price"), request.getMinPrice()));
        }
        if (request.getMaxPrice() != null) {
            specs.add((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("price"), request.getMaxPrice()));
        }

        // Active books only by default
        specs.add((root, query, cb) -> cb.isTrue(root.get("active")));

        return specs.stream().reduce(Specification.where(null), Specification::and);
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String field = switch (sortBy != null ? sortBy : "createdAt") {
            case "title" -> "title";
            case "price" -> "price";
            case "rating" -> "averageRating";
            case "publishedDate" -> "publicationDate";
            default -> "createdAt";
        };
        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }

    public BookResponse toResponse(Book book, int availableQty) {
        return BookResponse.builder()
            .id(book.getId())
            .isbn(book.getIsbn())
            .title(book.getTitle())
            .description(book.getDescription())
            .authors(book.getAuthors().stream().map(Author::getName).toList())
            .publisher(book.getPublisher())
            .publicationDate(book.getPublicationDate())
            .language(book.getLanguage())
            .pages(book.getPages())
            .price(book.getPrice())
            .currency(book.getCurrency())
            .discountPercent(book.getDiscountPercent())
            .effectivePrice(book.getEffectivePrice())
            .coverImageUrl(book.getCoverImageUrl())
            .averageRating(book.getAverageRating())
            .reviewCount(book.getReviewCount())
            .active(book.isActive())
            .category(book.getCategory() != null ? BookResponse.CategoryInfo.builder()
                .id(book.getCategory().getId())
                .name(book.getCategory().getName())
                .slug(book.getCategory().getSlug())
                .build() : null)
            .availableQuantity(availableQty)
            .createdAt(book.getCreatedAt())
            .updatedAt(book.getUpdatedAt())
            .build();
    }
}
