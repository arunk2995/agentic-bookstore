package com.bookstore.service;

import com.bookstore.config.BookstoreProperties;
import com.bookstore.domain.catalog.Book;
import com.bookstore.domain.inventory.*;
import com.bookstore.dto.inventory.*;
import com.bookstore.exception.BookstoreException;
import com.bookstore.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BookstoreProperties properties;

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(UUID bookId) {
        Inventory inv = inventoryRepository.findByBookId(bookId)
            .orElseThrow(() -> BookstoreException.notFound("Inventory", bookId));
        return toResponse(inv);
    }

    @Transactional
    public void adjustInventory(UUID bookId, int quantityChange, String reason, UUID performedByUserId) {
        Inventory inv = inventoryRepository.findByBookIdForUpdate(bookId)
            .orElseThrow(() -> BookstoreException.notFound("Inventory", bookId));

        int before = inv.getQuantityOnHand();

        if (quantityChange > 0) {
            inv.addStock(quantityChange);
        } else {
            int absChange = Math.abs(quantityChange);
            if (inv.getQuantityAvailable() < absChange) {
                throw BookstoreException.insufficientInventory(
                    "Cannot reduce inventory below available stock");
            }
            inv.setQuantityOnHand(inv.getQuantityOnHand() + quantityChange);
            inv.setQuantityAvailable(inv.getQuantityAvailable() + quantityChange);
        }

        inventoryRepository.save(inv);

        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> BookstoreException.notFound("Book", bookId));

        InventoryMovement movement = InventoryMovement.builder()
            .book(book)
            .movementType(quantityChange > 0 ? MovementType.RESTOCK : MovementType.ADJUSTMENT)
            .quantityChange(quantityChange)
            .quantityBefore(before)
            .quantityAfter(inv.getQuantityOnHand())
            .reason(reason)
            .build();
        movementRepository.save(movement);

        log.info("Inventory adjusted for book {}: {} -> {}", bookId, before, inv.getQuantityOnHand());
    }

    @Transactional
    public void reserveStock(UUID bookId, UUID orderId, int quantity) {
        Inventory inv = inventoryRepository.findByBookIdForUpdate(bookId)
            .orElseThrow(() -> BookstoreException.notFound("Inventory", bookId));

        if (inv.getQuantityAvailable() < quantity) {
            throw BookstoreException.insufficientInventory(
                "Insufficient stock for book: " + bookId + ". Available: " + inv.getQuantityAvailable());
        }

        inv.reserve(quantity);
        inventoryRepository.save(inv);

        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> BookstoreException.notFound("Book", bookId));

        InventoryReservation reservation = InventoryReservation.builder()
            .book(book)
            .orderId(orderId)
            .quantity(quantity)
            .status(ReservationStatus.ACTIVE)
            .expiresAt(Instant.now().plus(
                properties.getInventory().getReservationExpiryMinutes(), ChronoUnit.MINUTES))
            .build();
        reservationRepository.save(reservation);

        // Record movement
        InventoryMovement movement = InventoryMovement.builder()
            .book(book)
            .movementType(MovementType.RESERVATION)
            .quantityChange(-quantity)
            .quantityBefore(inv.getQuantityAvailable() + quantity)
            .quantityAfter(inv.getQuantityAvailable())
            .reason("Order reservation: " + orderId)
            .referenceId(orderId.toString())
            .build();
        movementRepository.save(movement);
    }

    @Transactional
    public void releaseReservation(UUID orderId) {
        List<InventoryReservation> reservations =
            reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);

        for (InventoryReservation reservation : reservations) {
            Inventory inv = inventoryRepository.findByBookIdForUpdate(reservation.getBook().getId())
                .orElseThrow(() -> BookstoreException.notFound("Inventory", reservation.getBook().getId()));

            inv.releaseReservation(reservation.getQuantity());
            inventoryRepository.save(inv);
            reservation.setStatus(ReservationStatus.RELEASED);
            reservationRepository.save(reservation);
        }
    }

    @Transactional
    public void fulfillReservation(UUID orderId) {
        List<InventoryReservation> reservations =
            reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);

        for (InventoryReservation reservation : reservations) {
            Inventory inv = inventoryRepository.findByBookIdForUpdate(reservation.getBook().getId())
                .orElseThrow(() -> BookstoreException.notFound("Inventory", reservation.getBook().getId()));

            inv.fulfillReservation(reservation.getQuantity());
            inventoryRepository.save(inv);
            reservation.setStatus(ReservationStatus.FULFILLED);
            reservationRepository.save(reservation);

            InventoryMovement movement = InventoryMovement.builder()
                .book(reservation.getBook())
                .movementType(MovementType.SALE)
                .quantityChange(-reservation.getQuantity())
                .quantityBefore(inv.getQuantityOnHand() + reservation.getQuantity())
                .quantityAfter(inv.getQuantityOnHand())
                .reason("Order fulfillment: " + orderId)
                .referenceId(orderId.toString())
                .build();
            movementRepository.save(movement);
        }
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockBooks(int threshold) {
        return inventoryRepository.findLowStockItems(threshold).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<InventoryMovementResponse> getMovements(UUID bookId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return movementRepository.findByBookIdOrderByCreatedAtDesc(bookId, pageable)
            .map(this::toMovementResponse);
    }

    @Scheduled(fixedDelay = 300000) // every 5 minutes
    @Transactional
    public void expireReservations() {
        int expired = reservationRepository.expireReservations(Instant.now());
        if (expired > 0) {
            log.info("Expired {} inventory reservations", expired);
        }
    }

    private InventoryResponse toResponse(Inventory inv) {
        return InventoryResponse.builder()
            .id(inv.getId())
            .bookId(inv.getBook().getId())
            .bookTitle(inv.getBook().getTitle())
            .quantityOnHand(inv.getQuantityOnHand())
            .quantityReserved(inv.getQuantityReserved())
            .quantityAvailable(inv.getQuantityAvailable())
            .reorderPoint(inv.getReorderPoint())
            .lowStock(inv.isLowStock(properties.getInventory().getLowStockThreshold()))
            .updatedAt(inv.getUpdatedAt())
            .build();
    }

    private InventoryMovementResponse toMovementResponse(InventoryMovement m) {
        return InventoryMovementResponse.builder()
            .id(m.getId())
            .bookId(m.getBook().getId())
            .movementType(m.getMovementType())
            .quantityChange(m.getQuantityChange())
            .quantityBefore(m.getQuantityBefore())
            .quantityAfter(m.getQuantityAfter())
            .reason(m.getReason())
            .referenceId(m.getReferenceId())
            .createdAt(m.getCreatedAt())
            .build();
    }
}
