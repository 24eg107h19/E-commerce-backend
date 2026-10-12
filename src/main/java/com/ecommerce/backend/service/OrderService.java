package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.OrderRequest;
import com.ecommerce.backend.model.*;
import com.ecommerce.backend.repository.CartItemRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
@Transactional
public class OrderService {

    private static final Set<String> ORDER_STATUSES = Set.of(
            "PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED");

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final EntityManager entityManager;

    public OrderService(OrderRepository orderRepository,
                        CartItemRepository cartItemRepository,
                        UserRepository userRepository,
                        ProductRepository productRepository,
                        EntityManager entityManager) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.entityManager = entityManager;
    }

    public Order placeOrder(OrderRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("User id is required to place an order.");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        List<CartItem> cartItems = new ArrayList<>(cartItemRepository.findByUserId(user.getId()));
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty. Cannot place order.");
        }

        Map<Long, Integer> requestedQuantities = new TreeMap<>();
        Map<Long, Product> products = new TreeMap<>();
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            if (product == null || product.getId() == null) {
                throw new IllegalArgumentException("Cart items must reference an existing product.");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Cart item quantities must be greater than zero.");
            }
            try {
                Long productId = product.getId();
                requestedQuantities.merge(productId, item.getQuantity(), Math::addExact);
                products.putIfAbsent(productId, product);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("Requested product quantity is too large.", e);
            }
        }

        double totalAmount = 0.0;
        for (Map.Entry<Long, Integer> requested : requestedQuantities.entrySet()) {
            Product product = products.get(requested.getKey());
            entityManager.refresh(product, LockModeType.PESSIMISTIC_WRITE);
            if (product.getQuantity() < requested.getValue()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }
            totalAmount += product.getPrice() * requested.getValue();
        }

        Order order = new Order(
                user,
                totalAmount,
                "PENDING",
                request.getShippingAddress(),
                request.getPhoneNumber()
        );

        for (Map.Entry<Long, Integer> requested : requestedQuantities.entrySet()) {
            Product product = products.get(requested.getKey());
            product.setQuantity(product.getQuantity() - requested.getValue());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem(
                    order, product, requested.getValue(), product.getPrice());
            order.addOrderItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        // Clear user's cart after placing order
        cartItemRepository.deleteByUserId(user.getId());

        return savedOrder;
    }

    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    public Order updateOrderStatus(Long orderId, String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Order status is required.");
        }
        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!ORDER_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid order status: " + status);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        order.setStatus(normalizedStatus);
        return orderRepository.save(order);
    }
}
