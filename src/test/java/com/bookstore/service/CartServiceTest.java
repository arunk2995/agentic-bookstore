package com.bookstore.service;

import com.bookstore.domain.cart.Cart;
import com.bookstore.domain.cart.CartItem;
import com.bookstore.domain.catalog.Book;
import com.bookstore.domain.inventory.Inventory;
import com.bookstore.domain.user.User;
import com.bookstore.dto.cart.AddToCartRequest;
import com.bookstore.dto.cart.CartResponse;
import com.bookstore.exception.BookstoreException;
import com.bookstore.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Unit Tests")
class CartServiceTest {

    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock BookRepository bookRepository;
    @Mock InventoryRepository inventoryRepository;
    @Mock UserRepository userRepository;

    @InjectMocks CartService cartService;

    private UUID userId;
    private User user;
    private Book book;
    private Cart cart;
    private Inventory inventory;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).username("testuser").email("test@test.com").build();
        book = Book.builder()
            .id(UUID.randomUUID())
            .isbn("978-0000000001")
            .title("Test Book")
            .price(BigDecimal.valueOf(500))
            .discountPercent(BigDecimal.ZERO)
            .currency("INR")
            .active(true)
            .build();
        cart = Cart.builder().id(UUID.randomUUID()).user(user).items(new ArrayList<>()).build();
        inventory = Inventory.builder()
            .id(UUID.randomUUID())
            .book(book)
            .quantityOnHand(50)
            .quantityAvailable(50)
            .quantityReserved(0)
            .build();
    }

    @Test
    @DisplayName("addItem adds new item to cart with correct server-side price")
    void addItem_newBook_addsWithCurrentPrice() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(inventoryRepository.findByBookId(book.getId())).thenReturn(Optional.of(inventory));
        when(cartRepository.save(any())).thenReturn(cart);

        AddToCartRequest request = new AddToCartRequest();
        request.setBookId(book.getId());
        request.setQuantity(2);

        cartService.addItem(userId, request);

        assertThat(cart.getItems()).hasSize(1);
        CartItem addedItem = cart.getItems().get(0);
        assertThat(addedItem.getQuantity()).isEqualTo(2);
        assertThat(addedItem.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(addedItem.getLineTotal()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    @DisplayName("addItem throws when insufficient stock")
    void addItem_insufficientStock_throwsException() {
        inventory.setQuantityAvailable(1);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(inventoryRepository.findByBookId(book.getId())).thenReturn(Optional.of(inventory));

        AddToCartRequest request = new AddToCartRequest();
        request.setBookId(book.getId());
        request.setQuantity(5);

        assertThatThrownBy(() -> cartService.addItem(userId, request))
            .isInstanceOf(BookstoreException.class)
            .hasMessageContaining("Only 1 copies available");
    }

    @Test
    @DisplayName("cart subtotal calculates correctly")
    void cart_subtotal_calculatesCorrectly() {
        CartItem item1 = CartItem.builder()
            .book(book)
            .quantity(2)
            .unitPrice(BigDecimal.valueOf(500))
            .currency("INR")
            .build();
        CartItem item2 = CartItem.builder()
            .book(book)
            .quantity(1)
            .unitPrice(BigDecimal.valueOf(300))
            .currency("INR")
            .build();
        cart.getItems().add(item1);
        cart.getItems().add(item2);

        BigDecimal subtotal = cart.getSubtotal();
        assertThat(subtotal).isEqualByComparingTo(BigDecimal.valueOf(1300));
    }

    @Test
    @DisplayName("addItem throws when book is inactive")
    void addItem_inactiveBook_throwsException() {
        book.setActive(false);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

        AddToCartRequest request = new AddToCartRequest();
        request.setBookId(book.getId());
        request.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem(userId, request))
            .isInstanceOf(BookstoreException.class)
            .hasMessageContaining("not available");
    }
}
