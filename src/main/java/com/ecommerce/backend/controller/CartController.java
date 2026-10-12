package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.CartItemRequest;
import com.ecommerce.backend.model.CartItem;
import com.ecommerce.backend.service.CartService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<CartItem>> getCartItems(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            return ResponseEntity.ok(cartService.getCartItems(userId, authenticatedUserId(jwt)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(
            @RequestBody CartItemRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            CartItem item = cartService.addToCart(request, authenticatedUserId(jwt));
            return ResponseEntity.status(HttpStatus.CREATED).body(item);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{cartItemId}")
    public ResponseEntity<?> updateQuantity(
            @PathVariable Long cartItemId,
            @RequestBody Map<String, Integer> payload,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            Integer quantity = payload.get("quantity");
            if (quantity == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Quantity is required"));
            }
            CartItem updated = cartService.updateQuantity(cartItemId, quantity, authenticatedUserId(jwt));
            return ResponseEntity.ok(updated != null ? updated : Map.of("message", "Item removed from cart"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<?> removeFromCart(
            @PathVariable Long cartItemId,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            cartService.removeFromCart(cartItemId, authenticatedUserId(jwt));
            return ResponseEntity.ok(Map.of("message", "Item removed from cart"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<?> clearCart(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            cartService.clearCart(userId, authenticatedUserId(jwt));
            return ResponseEntity.ok(Map.of("message", "Cart cleared successfully"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    private Long authenticatedUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
