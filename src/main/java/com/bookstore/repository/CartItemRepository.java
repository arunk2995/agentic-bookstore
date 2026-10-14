package com.bookstore.repository;

import com.bookstore.domain.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    Optional<CartItem> findByCartIdAndBookId(UUID cartId, UUID bookId);
    void deleteByCartId(UUID cartId);
}
