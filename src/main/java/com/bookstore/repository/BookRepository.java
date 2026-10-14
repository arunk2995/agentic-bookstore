package com.bookstore.repository;

import com.bookstore.domain.catalog.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID>, JpaSpecificationExecutor<Book> {
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);
    boolean existsByIsbnAndIdNot(String isbn, UUID id);

    @Query("SELECT b FROM Book b WHERE b.active = true AND b.category.id = :categoryId")
    Page<Book> findByCategoryId(UUID categoryId, Pageable pageable);

    @Query("""
        SELECT b FROM Book b
        JOIN b.authors a
        WHERE b.active = true
        AND LOWER(a.name) LIKE LOWER(CONCAT('%', :authorName, '%'))
        """)
    Page<Book> findByAuthorName(String authorName, Pageable pageable);

    @Query("""
        SELECT b FROM Book b
        WHERE b.active = true
        AND b.category.id = :categoryId
        AND b.id <> :bookId
        ORDER BY b.averageRating DESC
        """)
    List<Book> findRelatedBooks(UUID bookId, UUID categoryId, Pageable pageable);

    @Query("""
        SELECT b FROM Book b
        WHERE b.active = true
        AND (:category IS NULL OR b.category.id = :category)
        ORDER BY b.averageRating DESC
        """)
    Page<Book> findTopRated(UUID category, Pageable pageable);

    @Query("""
        SELECT b FROM Book b
        WHERE b.active = true
        AND b.price BETWEEN :minPrice AND :maxPrice
        """)
    Page<Book> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}
