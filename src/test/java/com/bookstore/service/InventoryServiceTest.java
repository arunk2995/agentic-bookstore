package com.bookstore.service;

import com.bookstore.domain.inventory.Inventory;
import com.bookstore.domain.catalog.Book;
import com.bookstore.exception.BookstoreException;
import com.bookstore.repository.*;
import com.bookstore.config.BookstoreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Unit Tests")
class InventoryServiceTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock InventoryMovementRepository movementRepository;
    @Mock InventoryReservationRepository reservationRepository;
    @Mock BookRepository bookRepository;
    @Mock BookstoreProperties properties;

    @InjectMocks InventoryService inventoryService;

    private Book book;
    private Inventory inventory;

    @BeforeEach
    void setup() {
        book = Book.builder()
            .id(UUID.randomUUID())
            .isbn("978-0000000001")
            .title("Test Book")
            .price(BigDecimal.valueOf(500))
            .build();

        inventory = Inventory.builder()
            .id(UUID.randomUUID())
            .book(book)
            .quantityOnHand(100)
            .quantityAvailable(100)
            .quantityReserved(0)
            .build();

        var inventoryProps = new BookstoreProperties.Inventory();
        when(properties.getInventory()).thenReturn(inventoryProps);
    }

    @Test
    @DisplayName("reserveStock reduces available quantity")
    void reserveStock_reducesAvailableQuantity() {
        when(inventoryRepository.findByBookIdForUpdate(book.getId())).thenReturn(Optional.of(inventory));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

        UUID orderId = UUID.randomUUID();
        inventoryService.reserveStock(book.getId(), orderId, 10);

        assertThat(inventory.getQuantityAvailable()).isEqualTo(90);
        assertThat(inventory.getQuantityReserved()).isEqualTo(10);
    }

    @Test
    @DisplayName("reserveStock throws when insufficient stock")
    void reserveStock_insufficientStock_throwsException() {
        when(inventoryRepository.findByBookIdForUpdate(book.getId())).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.reserveStock(book.getId(), UUID.randomUUID(), 200))
            .isInstanceOf(BookstoreException.class)
            .hasMessageContaining("Insufficient");
    }

    @Test
    @DisplayName("inventory.reserve prevents negative available quantity")
    void inventory_reserve_preventsNegativeAvailable() {
        assertThatThrownBy(() -> inventory.reserve(200))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Insufficient inventory");
    }

    @Test
    @DisplayName("inventory.releaseReservation restores available quantity")
    void inventory_releaseReservation_restoresQuantity() {
        inventory.reserve(20);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(80);

        inventory.releaseReservation(20);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(100);
        assertThat(inventory.getQuantityReserved()).isEqualTo(0);
    }

    @Test
    @DisplayName("getLowStockBooks returns items below threshold")
    void getLowStockBooks_returnsBelowThreshold() {
        inventory.setQuantityAvailable(5);
        when(inventoryRepository.findLowStockItems(10)).thenReturn(java.util.List.of(inventory));

        var result = inventoryService.getLowStockBooks(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isLowStock()).isTrue();
    }
}
