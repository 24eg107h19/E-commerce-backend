package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.OrderRequest;
import com.ecommerce.backend.model.CartItem;
import com.ecommerce.backend.model.Order;
import com.ecommerce.backend.model.OrderItem;
import com.ecommerce.backend.model.Product;
import com.ecommerce.backend.model.User;
import com.ecommerce.backend.repository.CartItemRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrderAggregatesDuplicateCartItemsAndLocksStock() {
        User user = new User();
        user.setId(7L);
        Product product = new Product("Keyboard", "", 12.5, 10, "Electronics", "");
        product.setId(3L);
        CartItem firstItem = new CartItem(user, product, 2);
        CartItem secondItem = new CartItem(user, product, 3);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUserId(7L)).thenReturn(List.of(firstItem, secondItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> {
            Product lockedProduct = invocation.getArgument(0);
            lockedProduct.setQuantity(10);
            return null;
        }).when(entityManager).refresh(product, LockModeType.PESSIMISTIC_WRITE);

        Order order = orderService.placeOrder(new OrderRequest(7L, "Address", "555-0100"));

        assertEquals(62.5, order.getTotalAmount());
        assertEquals(5, product.getQuantity());
        assertEquals(1, order.getOrderItems().size());
        OrderItem orderItem = order.getOrderItems().get(0);
        assertEquals(5, orderItem.getQuantity());
        assertEquals(12.5, orderItem.getPrice());
        verify(entityManager).refresh(product, LockModeType.PESSIMISTIC_WRITE);
        verify(cartItemRepository).deleteByUserId(7L);
    }

    @Test
    void placeOrderRejectsNonPositiveCartQuantityWithoutChangingStock() {
        User user = new User();
        user.setId(7L);
        Product product = new Product("Keyboard", "", 12.5, 10, "Electronics", "");
        product.setId(3L);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUserId(7L)).thenReturn(List.of(new CartItem(user, product, -2)));

        assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(new OrderRequest(7L, "Address", "555-0100")));

        assertEquals(10, product.getQuantity());
        verify(entityManager, never()).refresh(product, LockModeType.PESSIMISTIC_WRITE);
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartItemRepository, never()).deleteByUserId(7L);
    }

    @Test
    void updateOrderStatusRejectsUnknownStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> orderService.updateOrderStatus(1L, "RETURNED"));

        verify(orderRepository, never()).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatusTrimsAndNormalizesStatus() {
        Order order = new Order();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updated = orderService.updateOrderStatus(1L, " shipped ");

        assertEquals("SHIPPED", updated.getStatus());
        ArgumentCaptor<Order> savedOrder = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(savedOrder.capture());
        assertEquals("SHIPPED", savedOrder.getValue().getStatus());
    }
}
