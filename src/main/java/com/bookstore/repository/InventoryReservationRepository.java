package com.bookstore.repository;

import com.bookstore.domain.inventory.InventoryReservation;
import com.bookstore.domain.inventory.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    List<InventoryReservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    @Modifying
    @Query("UPDATE InventoryReservation r SET r.status = com.bookstore.domain.inventory.ReservationStatus.EXPIRED WHERE r.expiresAt < :now AND r.status = com.bookstore.domain.inventory.ReservationStatus.ACTIVE")
    int expireReservations(Instant now);
}
