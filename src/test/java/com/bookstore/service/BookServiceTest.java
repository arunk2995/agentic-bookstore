package com.bookstore.service;

import com.bookstore.domain.catalog.Author;
import com.bookstore.domain.catalog.Book;
import com.bookstore.domain.inventory.Inventory;
import com.bookstore.dto.book.BookSearchRequest;
import com.bookstore.dto.book.CreateBookRequest;
import com.bookstore.dto.book.BookResponse;
import com.bookstore.exception.BookstoreException;
import com.bookstore.repository.AuthorRepository;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CategoryRepository;
import com.bookstore.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService Unit Tests")
class BookServiceTest {

    @Mock BookRepository bookRepository;
    @Mock AuthorRepository authorRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock InventoryRepository inventoryRepository;

    @InjectMocks BookService bookService;

    private Book sampleBook;
    private Inventory sampleInventory;

    @BeforeEach
    void setup() {
        Author author = Author.builder().id(UUID.randomUUID()).name("Joshua Bloch").build();
        sampleBook = Book.builder()
            .id(UUID.randomUUID())
            .isbn("978-0134685991")
            .title("Effective Java")
            .price(new BigDecimal("899.00"))
            .currency("INR")
            .discountPercent(BigDecimal.ZERO)
            .active(true)
            .build();
        sampleBook.addAuthor(author);

        sampleInventory = Inventory.builder()
            .id(UUID.randomUUID())
            .book(sampleBook)
            .quantityOnHand(50)
            .quantityAvailable(50)
            .quantityReserved(0)
            .build();
    }

    @Test
    @DisplayName("getBook returns book response when book exists")
    void getBook_whenExists_returnsResponse() {
        when(bookRepository.findById(sampleBook.getId())).thenReturn(Optional.of(sampleBook));
        when(inventoryRepository.findByBookId(sampleBook.getId())).thenReturn(Optional.of(sampleInventory));

        BookResponse response = bookService.getBook(sampleBook.getId());

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Effective Java");
        assertThat(response.getIsbn()).isEqualTo("978-0134685991");
        assertThat(response.getAvailableQuantity()).isEqualTo(50);
        assertThat(response.getAuthors()).contains("Joshua Bloch");
    }

    @Test
    @DisplayName("getBook throws NOT_FOUND when book does not exist")
    void getBook_whenNotFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBook(id))
            .isInstanceOf(BookstoreException.class)
            .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("createBook throws conflict when ISBN already exists")
    void createBook_withDuplicateIsbn_throwsConflict() {
        CreateBookRequest request = new CreateBookRequest();
        request.setIsbn("978-0134685991");
        request.setTitle("Test Book");
        request.setAuthors(List.of("Author One"));
        request.setPrice(new BigDecimal("499.00"));

        when(bookRepository.existsByIsbn("978-0134685991")).thenReturn(true);

        assertThatThrownBy(() -> bookService.createBook(request))
            .isInstanceOf(BookstoreException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("getEffectivePrice calculates discount correctly")
    void book_effectivePrice_withDiscount() {
        sampleBook.setPrice(new BigDecimal("1000.00"));
        sampleBook.setDiscountPercent(new BigDecimal("10.00"));

        BigDecimal effective = sampleBook.getEffectivePrice();
        assertThat(effective).isEqualByComparingTo(new BigDecimal("900.00"));
    }

    @Test
    @DisplayName("searchBooks returns paginated results")
    void searchBooks_returnsPagedResults() {
        Page<Book> bookPage = new PageImpl<>(List.of(sampleBook));
        when(bookRepository.findAll(any(Specification.class), any())).thenReturn(bookPage);
        when(inventoryRepository.findByBookId(any())).thenReturn(Optional.of(sampleInventory));

        BookSearchRequest request = BookSearchRequest.builder()
            .query("java")
            .page(0)
            .size(10)
            .build();

        Page<BookResponse> result = bookService.searchBooks(request);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Effective Java");
    }
}
