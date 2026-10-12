package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.CartItemRequest;
import com.ecommerce.backend.model.CartItem;
import com.ecommerce.backend.model.Product;
import com.ecommerce.backend.model.User;
import com.ecommerce.backend.repository.CartItemRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository,
                       UserRepository userRepository,
                       ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public List<CartItem> getCartItems(Long userId, Long authenticatedUserId) {
        requireOwner(userId, authenticatedUserId);
        return cartItemRepository.findByUserId(userId);
    }

    public CartItem addToCart(CartItemRequest request, Long authenticatedUserId) {
        requireOwner(request.getUserId(), authenticatedUserId);
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + request.getProductId()));

        Optional<CartItem> existingItemOpt = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId());

        if (existingItemOpt.isPresent()) {
            CartItem item = existingItemOpt.get();
            int newQuantity = item.getQuantity() + (request.getQuantity() != null ? request.getQuantity() : 1);
            item.setQuantity(newQuantity);
            return cartItemRepository.save(item);
        } else {
            int quantity = (request.getQuantity() != null && request.getQuantity() > 0) ? request.getQuantity() : 1;
            CartItem newItem = new CartItem(user, product, quantity);
            return cartItemRepository.save(newItem);
        }
    }

    public CartItem updateQuantity(Long cartItemId, int quantity, Long authenticatedUserId) {
        CartItem cartItem = cartItemRepository.findByIdAndUserId(cartItemId, authenticatedUserId)
                .orElseThrow(() -> new RuntimeException("Cart item not found with id: " + cartItemId));

        if (quantity <= 0) {
            cartItemRepository.delete(cartItem);
            return null;
        }

        cartItem.setQuantity(quantity);
        return cartItemRepository.save(cartItem);
    }

    public void removeFromCart(Long cartItemId, Long authenticatedUserId) {
        CartItem cartItem = cartItemRepository.findByIdAndUserId(cartItemId, authenticatedUserId)
                .orElseThrow(() -> new RuntimeException("Cart item not found with id: " + cartItemId));
        cartItemRepository.delete(cartItem);
    }

    public void clearCart(Long userId, Long authenticatedUserId) {
        requireOwner(userId, authenticatedUserId);
        cartItemRepository.deleteByUserId(userId);
    }

    private void requireOwner(Long requestedUserId, Long authenticatedUserId) {
        if (requestedUserId == null || !requestedUserId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("You may only access your own cart.");
        }
    }
}
