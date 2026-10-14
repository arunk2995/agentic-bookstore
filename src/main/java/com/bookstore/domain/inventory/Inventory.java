package com.bookstore.domain.inventory;

import com.bookstore.domain.catalog.Book;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory",
    indexes = @Index(name = "idx_inventory_book_id", columnList = "book_id"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false, unique = true)
    private Book book;

    @Column(name = "quantity_on_hand", nullable = false)
    @Builder.Default
    private int quantityOnHand = 0;

    @Column(name = "quantity_reserved", nullable = false)
    @Builder.Default
    private int quantityReserved = 0;

    @Column(name = "quantity_available", nullable = false)
    @Builder.Default
    private int quantityAvailable = 0;

    @Column(name = "reorder_point")
    @Builder.Default
    private int reorderPoint = 10;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private long version = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void reserve(int quantity) {
        if (quantityAvailable < quantity) {
            throw new IllegalStateException("Insufficient inventory for book: " + book.getId());
        }
        quantityReserved += quantity;
        quantityAvailable -= quantity;
    }

    public void releaseReservation(int quantity) {
        int toRelease = Math.min(quantity, quantityReserved);
        quantityReserved -= toRelease;
        quantityAvailable += toRelease;
    }

    public void fulfillReservation(int quantity) {
        int toFulfill = Math.min(quantity, quantityReserved);
        quantityReserved -= toFulfill;
        quantityOnHand -= toFulfill;
    }

    public void addStock(int quantity) {
        quantityOnHand += quantity;
        quantityAvailable += quantity;
    }

    public boolean isLowStock(int threshold) {
        return quantityAvailable <= threshold;
    }
}
