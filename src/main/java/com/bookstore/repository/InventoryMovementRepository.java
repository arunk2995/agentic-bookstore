package com.bookstore.repository;

import com.bookstore.domain.inventory.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {
    Page<InventoryMovement> findByBookIdOrderByCreatedAtDesc(UUID bookId, Pageable pageable);
}
