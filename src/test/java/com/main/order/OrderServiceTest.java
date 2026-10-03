package com.main.order;

import com.main.notification.NotificationService;
import com.main.product.Product;
import com.main.product.ProductRepository;
import com.main.shared.exception.BusinessException;
import com.main.user.User;
import com.main.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private OrderMapper orderMapper = new OrderMapper();

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User sampleUser;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .firstName("Arjun")
                .lastName("Dhatbale")
                .email("arjun@example.com")
                .phone("9876543210")
                .role(User.UserRole.ADMIN)
                .status(User.UserStatus.ACTIVE)
                .build();

        sampleProduct = Product.builder()
                .id(101L)
                .name("Wireless Mouse")
                .description("Ergonomic mouse")
                .price(new BigDecimal("1500.00"))
                .stock(10)
                .category("Electronics")
                .status(Product.ProductStatus.ACTIVE)
                .build();
    }

    @Test
    void testCreateOrder_success_deductsStock() {
        OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                .userId(1L)
                .shippingAddress("123 Tech Park, Pune")
                .contactPhone("9876543210")
                .paymentMethod("UPI")
                .items(List.of(
                        OrderItemRequestDto.builder()
                                .productId(101L)
                                .quantity(2)
                                .build()
                ))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(1001L);
            return o;
        });

        OrderResponseDto response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertEquals(new BigDecimal("3000.00"), response.getTotalAmount());
        assertEquals(8, sampleProduct.getStock()); // stock decremented from 10 to 8
        verify(productRepository).save(sampleProduct);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testCreateOrder_insufficientStock_throwsException() {
        OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                .userId(1L)
                .shippingAddress("123 Tech Park, Pune")
                .contactPhone("9876543210")
                .paymentMethod("UPI")
                .items(List.of(
                        OrderItemRequestDto.builder()
                                .productId(101L)
                                .quantity(15) // more than 10
                                .build()
                ))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(sampleProduct));

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createOrder(request));
        assertEquals("INSUFFICIENT_STOCK", ex.getErrorCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCancelOrder_success_restoresStock() {
        Order order = Order.builder()
                .id(50L)
                .orderNumber("ORD-TEST-001")
                .user(sampleUser)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("3000.00"))
                .items(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .order(order)
                .product(sampleProduct)
                .productName(sampleProduct.getName())
                .productPrice(sampleProduct.getPrice())
                .quantity(2)
                .subtotal(new BigDecimal("3000.00"))
                .build();
        order.addItem(item);

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDto response = orderService.cancelOrder(50L, 1L);

        assertEquals("CANCELLED", response.getStatus());
        assertEquals(12, sampleProduct.getStock()); // restored 10 + 2 = 12
        verify(productRepository).save(sampleProduct);
    }
}
