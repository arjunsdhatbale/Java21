package com.main.payment;

import com.main.notification.NotificationService;
import com.main.order.Order;
import com.main.order.OrderItem;
import com.main.order.OrderRepository;
import com.main.order.OrderStatus;
import com.main.product.Product;
import com.main.product.ProductRepository;
import com.main.shared.PaymentStatus;
import com.main.shared.exception.BusinessException;
import com.main.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Spy
    private PaymentMapper paymentMapper = new PaymentMapper();

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User sampleUser;
    private Product sampleProduct;
    private Order sampleOrder;

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
                .price(new BigDecimal("1500.00"))
                .stock(10)
                .category("Electronics")
                .status(Product.ProductStatus.ACTIVE)
                .build();

        sampleOrder = Order.builder()
                .id(201L)
                .orderNumber("ORD-TEST-201")
                .user(sampleUser)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod("UPI")
                .totalAmount(new BigDecimal("1500.00"))
                .shippingAddress("123 Tech Park, Pune")
                .contactPhone("9876543210")
                .items(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .order(sampleOrder)
                .product(sampleProduct)
                .productName(sampleProduct.getName())
                .productPrice(sampleProduct.getPrice())
                .quantity(1)
                .subtotal(new BigDecimal("1500.00"))
                .build();
        sampleOrder.addItem(item);
    }

    @Test
    void testProcessPayment_success_updatesOrderToPaidAndConfirmed() {
        PaymentProcessRequestDto request = PaymentProcessRequestDto.builder()
                .orderId(201L)
                .paymentMethod("UPI")
                .upiId("arjun@okaxis")
                .notes("Test UPI payment")
                .build();

        when(orderRepository.findById(201L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(501L);
            return p;
        });

        PaymentResponseDto response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals("PAID", response.getPaymentStatus());
        assertEquals("UPI", response.getPaymentMethod());
        assertEquals(new BigDecimal("1500.00"), response.getAmount());
        assertNotNull(response.getTransactionId());

        // Verify order status and payment status updated
        assertEquals(PaymentStatus.PAID, sampleOrder.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, sampleOrder.getStatus());
        verify(orderRepository).save(sampleOrder);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testProcessPayment_orderAlreadyPaid_throwsException() {
        sampleOrder.setPaymentStatus(PaymentStatus.PAID);

        PaymentProcessRequestDto request = PaymentProcessRequestDto.builder()
                .orderId(201L)
                .paymentMethod("UPI")
                .build();

        when(orderRepository.findById(201L)).thenReturn(Optional.of(sampleOrder));

        BusinessException ex = assertThrows(BusinessException.class, () -> paymentService.processPayment(request));
        assertEquals("ORDER_ALREADY_PAID", ex.getErrorCode());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testRefundPayment_success_updatesStatusAndRestoresStock() {
        Payment payment = Payment.builder()
                .id(501L)
                .transactionId("TXN-TEST-501")
                .order(sampleOrder)
                .user(sampleUser)
                .amount(new BigDecimal("1500.00"))
                .paymentMethod(PaymentMethod.UPI)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        sampleOrder.setPaymentStatus(PaymentStatus.PAID);
        sampleOrder.setStatus(OrderStatus.CONFIRMED);

        PaymentRefundRequestDto refundRequest = PaymentRefundRequestDto.builder()
                .reason("Customer requested refund")
                .build();

        when(paymentRepository.findById(501L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponseDto response = paymentService.refundPayment(501L, refundRequest);

        assertEquals("REFUNDED", response.getPaymentStatus());
        assertEquals(PaymentStatus.REFUNDED, sampleOrder.getPaymentStatus());
        assertEquals(OrderStatus.CANCELLED, sampleOrder.getStatus());
        assertEquals(11, sampleProduct.getStock()); // Restored 10 + 1 = 11
        verify(productRepository).save(sampleProduct);
        verify(orderRepository).save(sampleOrder);
    }
}
