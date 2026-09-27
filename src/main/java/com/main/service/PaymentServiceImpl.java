package com.main.service;

import com.main.mapper.PaymentMapper;
import com.main.model.dto.*;
import com.main.model.entity.*;
import com.main.repo.OrderRepository;
import com.main.repo.PaymentRepository;
import com.main.repo.ProductRepository;
import com.main.shared.exception.BusinessException;
import com.main.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentMapper paymentMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public PaymentResponseDto processPayment(PaymentProcessRequestDto request) {
        log.info("Processing payment for orderId: {}, method: {}", request.getOrderId(), request.getPaymentMethod());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Cannot process payment for a cancelled order", "ORDER_CANCELLED", HttpStatus.BAD_REQUEST);
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Order is already paid", "ORDER_ALREADY_PAID", HttpStatus.BAD_REQUEST);
        }

        PaymentMethod method = parsePaymentMethod(request.getPaymentMethod());
        String transactionId = generateTransactionId();
        String gatewayRef = "GW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PaymentStatus status;
        LocalDateTime paymentDate = null;

        if (method == PaymentMethod.CASH_ON_DELIVERY) {
            status = PaymentStatus.PENDING;
            order.setPaymentStatus(PaymentStatus.PENDING);
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
            }
        } else {
            status = PaymentStatus.PAID;
            paymentDate = LocalDateTime.now();
            order.setPaymentStatus(PaymentStatus.PAID);
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
            }
        }

        order.setPaymentMethod(method.name());
        orderRepository.save(order);

        Payment payment = Payment.builder()
                .transactionId(transactionId)
                .order(order)
                .user(order.getUser())
                .amount(order.getTotalAmount())
                .currency("INR")
                .paymentMethod(method)
                .paymentStatus(status)
                .gatewayReference(gatewayRef)
                .paymentDate(paymentDate)
                .notes(request.getNotes() != null ? request.getNotes() : "Payment processed via " + method.name())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment record created with transactionId: {}", savedPayment.getTransactionId());

        // Send real-time notification
        try {
            String customerEmail = order.getUser() != null ? order.getUser().getEmail() : "user";
            String title = status == PaymentStatus.PAID ? "Payment Successful" : "Order Placed (COD)";
            String message = status == PaymentStatus.PAID
                    ? "Payment of ₹" + savedPayment.getAmount() + " successfully completed for Order #" + order.getOrderNumber() + " (Txn: " + savedPayment.getTransactionId() + ")"
                    : "Order #" + order.getOrderNumber() + " confirmed with Cash on Delivery for ₹" + savedPayment.getAmount();

            NotificationDto notif = NotificationDto.builder()
                    .recipient(customerEmail)
                    .title(title)
                    .message(message)
                    .type(status == PaymentStatus.PAID ? NotificationType.PAYMENT : NotificationType.ORDER)
                    .timestamp(LocalDateTime.now())
                    .build();

            notificationService.broadcast(notif);
            if (customerEmail != null && !customerEmail.isBlank()) {
                notificationService.sendToUser(customerEmail, notif);
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch WebSocket notification for payment: {}", e.getMessage());
        }

        return paymentMapper.toDto(savedPayment);
    }

    @Override
    public PaymentResponseDto getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return paymentMapper.toDto(payment);
    }

    @Override
    public PaymentResponseDto getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for order id: " + orderId));
        return paymentMapper.toDto(payment);
    }

    @Override
    public List<PaymentResponseDto> getAllPayments(Long userId, Long orderId) {
        if (orderId != null) {
            return paymentRepository.findByOrderId(orderId)
                    .stream()
                    .map(paymentMapper::toDto)
                    .toList();
        }

        if (userId != null) {
            return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .map(paymentMapper::toDto)
                    .toList();
        }

        return paymentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(paymentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public PaymentResponseDto refundPayment(Long paymentId, PaymentRefundRequestDto request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessException("Payment is already refunded", "ALREADY_REFUNDED", HttpStatus.BAD_REQUEST);
        }

        if (payment.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BusinessException("Only completed payments can be refunded", "INVALID_REFUND_STATUS", HttpStatus.BAD_REQUEST);
        }

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        payment.setFailureReason("Refund reason: " + request.getReason());
        Payment updatedPayment = paymentRepository.save(payment);

        // Update associated order
        Order order = payment.getOrder();
        if (order != null) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            order.setStatus(OrderStatus.CANCELLED);

            // Restore product stock
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    Product product = item.getProduct();
                    if (product != null) {
                        int restored = product.getStock() + item.getQuantity();
                        product.setStock(restored);
                        if (product.getStatus() == Product.ProductStatus.OUT_OF_STOCK && restored > 0) {
                            product.setStatus(Product.ProductStatus.ACTIVE);
                        }
                        productRepository.save(product);
                    }
                }
            }
            orderRepository.save(order);
        }

        try {
            String customerEmail = payment.getUser() != null ? payment.getUser().getEmail() : "user";
            NotificationDto notif = NotificationDto.builder()
                    .recipient(customerEmail)
                    .title("Payment Refunded")
                    .message("Refund of ₹" + payment.getAmount() + " processed for Order #" + (order != null ? order.getOrderNumber() : "") + ". Reason: " + request.getReason())
                    .type(NotificationType.PAYMENT)
                    .timestamp(LocalDateTime.now())
                    .build();

            notificationService.broadcast(notif);
        } catch (Exception e) {
            log.warn("Failed to dispatch WebSocket notification for refund: {}", e.getMessage());
        }

        return paymentMapper.toDto(updatedPayment);
    }

    @Override
    public PaymentStatsDto getPaymentStats() {
        long total = paymentRepository.count();
        long successful = paymentRepository.countByPaymentStatus(PaymentStatus.PAID);
        long pending = paymentRepository.countByPaymentStatus(PaymentStatus.PENDING);
        long failed = paymentRepository.countByPaymentStatus(PaymentStatus.FAILED);
        long refunded = paymentRepository.countByPaymentStatus(PaymentStatus.REFUNDED);
        BigDecimal volume = paymentRepository.calculateTotalVolume();

        return PaymentStatsDto.builder()
                .totalTransactions(total)
                .successfulTransactions(successful)
                .pendingTransactions(pending)
                .failedTransactions(failed)
                .refundedTransactions(refunded)
                .totalVolume(volume != null ? volume : BigDecimal.ZERO)
                .build();
    }

    private PaymentMethod parsePaymentMethod(String methodStr) {
        if (methodStr == null || methodStr.isBlank()) {
            return PaymentMethod.UPI;
        }

        String normalized = methodStr.trim().toUpperCase().replace(" ", "_");
        if (normalized.contains("CASH") || normalized.contains("COD")) {
            return PaymentMethod.CASH_ON_DELIVERY;
        }
        if (normalized.contains("CREDIT")) {
            return PaymentMethod.CREDIT_CARD;
        }
        if (normalized.contains("DEBIT")) {
            return PaymentMethod.DEBIT_CARD;
        }
        if (normalized.contains("NET") || normalized.contains("BANK")) {
            return PaymentMethod.NET_BANKING;
        }
        if (normalized.contains("WALLET")) {
            return PaymentMethod.WALLET;
        }
        if (normalized.contains("UPI")) {
            return PaymentMethod.UPI;
        }

        try {
            return PaymentMethod.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return PaymentMethod.UPI;
        }
    }

    private String generateTransactionId() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomPart = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "TXN-" + datePart + "-" + randomPart;
    }
}
