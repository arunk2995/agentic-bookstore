package com.bookstore.service;

import com.bookstore.domain.cart.*;
import com.bookstore.domain.catalog.Book;
import com.bookstore.domain.inventory.Inventory;
import com.bookstore.domain.user.User;
import com.bookstore.dto.cart.*;
import com.bookstore.exception.BookstoreException;
import com.bookstore.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(UUID userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);

        Book book = bookRepository.findById(request.getBookId())
            .orElseThrow(() -> BookstoreException.notFound("Book", request.getBookId()));

        if (!book.isActive()) {
            throw BookstoreException.badRequest("Book is not available for purchase");
        }

        Inventory inventory = inventoryRepository.findByBookId(book.getId())
            .orElseThrow(() -> BookstoreException.notFound("Inventory", book.getId()));

        if (inventory.getQuantityAvailable() < request.getQuantity()) {
            throw BookstoreException.insufficientInventory(
                "Only " + inventory.getQuantityAvailable() + " copies available");
        }

        // Check if item already in cart
        cartItemRepository.findByCartIdAndBookId(cart.getId(), book.getId())
            .ifPresentOrElse(item -> {
                int newQty = item.getQuantity() + request.getQuantity();
                if (inventory.getQuantityAvailable() < newQty) {
                    throw BookstoreException.insufficientInventory(
                        "Cannot add " + request.getQuantity() + " more copies. Available: " + inventory.getQuantityAvailable());
                }
                item.setQuantity(newQty);
                // Always use current server-side price
                item.setUnitPrice(book.getEffectivePrice());
                cartItemRepository.save(item);
            }, () -> {
                CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .book(book)
                    .quantity(request.getQuantity())
                    .unitPrice(book.getEffectivePrice())
                    .currency(book.getCurrency())
                    .build();
                cart.getItems().add(newItem);
            });

        cartRepository.save(cart);
        return toResponse(cartRepository.findByUserId(userId).orElse(cart));
    }

    @Transactional
    public CartResponse updateItem(UUID userId, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cart.getItems().stream()
            .filter(i -> i.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> BookstoreException.notFound("Cart item", itemId));

        Inventory inventory = inventoryRepository.findByBookId(item.getBook().getId())
            .orElseThrow(() -> BookstoreException.notFound("Inventory", item.getBook().getId()));

        if (inventory.getQuantityAvailable() < request.getQuantity()) {
            throw BookstoreException.insufficientInventory(
                "Only " + inventory.getQuantityAvailable() + " copies available");
        }

        // Refresh price from catalog
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(item.getBook().getEffectivePrice());
        cartRepository.save(cart);

        return toResponse(cartRepository.findByUserId(userId).orElse(cart));
    }

    @Transactional
    public CartResponse removeItem(UUID userId, UUID itemId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().removeIf(i -> i.getId().equals(itemId));
        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Transactional
    public void clearCart(UUID userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    private Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> BookstoreException.notFound("User", userId));
            Cart cart = Cart.builder().user(user).build();
            return cartRepository.save(cart);
        });
    }

    private CartResponse toResponse(Cart cart) {
        List<CartResponse.CartItemResponse> itemResponses = cart.getItems().stream()
            .map(item -> {
                int available = inventoryRepository.findByBookId(item.getBook().getId())
                    .map(Inventory::getQuantityAvailable)
                    .orElse(0);
                return CartResponse.CartItemResponse.builder()
                    .id(item.getId())
                    .bookId(item.getBook().getId())
                    .bookTitle(item.getBook().getTitle())
                    .bookIsbn(item.getBook().getIsbn())
                    .coverImageUrl(item.getBook().getCoverImageUrl())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .lineTotal(item.getLineTotal())
                    .currency(item.getCurrency())
                    .availableQuantity(available)
                    .build();
            }).toList();

        return CartResponse.builder()
            .id(cart.getId())
            .items(itemResponses)
            .subtotal(cart.getSubtotal())
            .currency("INR")
            .totalItems(cart.getTotalItems())
            .updatedAt(cart.getUpdatedAt())
            .build();
    }
}
