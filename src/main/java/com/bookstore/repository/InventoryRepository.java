package com.bookstore.repository;

import com.bookstore.domain.inventory.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    Optional<Inventory> findByBookId(UUID bookId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.book.id = :bookId")
    Optional<Inventory> findByBookIdForUpdate(UUID bookId);

    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable <= :threshold")
    List<Inventory> findLowStockItems(int threshold);
}
