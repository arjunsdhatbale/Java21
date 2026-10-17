package com.main.order;

import com.main.notification.NotificationDto;
import com.main.notification.NotificationService;
import com.main.notification.NotificationType;
import com.main.product.Product;
import com.main.product.ProductRepository;
import com.main.shared.PaymentStatus;
import com.main.shared.exception.BusinessException;
import com.main.shared.exception.ResourceNotFoundException;
import com.main.user.User;
import com.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public OrderResponseDto createOrder(OrderCreateRequestDto request) {
        log.info("Processing order placement for userId: {}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Order must have at least one product item", "EMPTY_ORDER", HttpStatus.BAD_REQUEST);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .contactPhone(request.getContactPhone())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "Cash on Delivery")
                .paymentStatus(PaymentStatus.PENDING)
                .notes(request.getNotes())
                .items(new ArrayList<>())
                .build();

        for (OrderItemRequestDto itemDto : request.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemDto.getProductId()));

            if (product.getStatus() != Product.ProductStatus.ACTIVE) {
                throw new BusinessException(
                        "Product '" + product.getName() + "' is not available for purchase (status: " + product.getStatus() + ")",
                        "PRODUCT_UNAVAILABLE",
                        HttpStatus.BAD_REQUEST
                );
            }

            if (product.getStock() < itemDto.getQuantity()) {
                throw new BusinessException(
                        "Insufficient stock for '" + product.getName() + "'. Available: " + product.getStock() + ", requested: " + itemDto.getQuantity(),
                        "INSUFFICIENT_STOCK",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Deduct inventory
            int newStock = product.getStock() - itemDto.getQuantity();
            product.setStock(newStock);
            if (newStock == 0) {
                product.setStatus(Product.ProductStatus.OUT_OF_STOCK);
            }
            productRepository.save(product);

            BigDecimal itemPrice = product.getPrice();
            BigDecimal subtotal = itemPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .productPrice(itemPrice)
                    .quantity(itemDto.getQuantity())
                    .subtotal(subtotal)
                    .build();

            order.addItem(orderItem);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        log.info("Order placed successfully with orderNumber: {}", savedOrder.getOrderNumber());

        // Send real-time notification
        try {
            NotificationDto notif = NotificationDto.builder()
                    .recipient(user.getEmail())
                    .title("Order Placed Successfully")
                    .message("Order #" + savedOrder.getOrderNumber() + " with " + savedOrder.getItems().size() + " items placed for ₹" + savedOrder.getTotalAmount())
                    .type(NotificationType.ORDER)
                    .timestamp(LocalDateTime.now())
                    .build();

            notificationService.broadcast(notif);
            if (user.getEmail() != null) {
                notificationService.sendToUser(user.getEmail(), notif);
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch WebSocket notification for new order: {}", e.getMessage());
        }

        return orderMapper.toDto(savedOrder);
    }

    @Override
    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(orderMapper::toDto)
                .toList();
    }

    @Override
    public List<OrderResponseDto> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(orderMapper::toDto)
                .toList();
    }

    @Override
    public OrderResponseDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional
    public OrderResponseDto updateOrderStatus(Long id, OrderStatusUpdateRequestDto dto) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(dto.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid order status: " + dto.getStatus(), "INVALID_STATUS", HttpStatus.BAD_REQUEST);
        }

        OrderStatus oldStatus = order.getStatus();

        // If status changed to CANCELLED, restore stock
        if (newStatus == OrderStatus.CANCELLED && oldStatus != OrderStatus.CANCELLED) {
            restoreStock(order);
        }

        if (newStatus == OrderStatus.DELIVERED && order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        // Notify user about status change
        try {
            String recipientEmail = order.getUser() != null ? order.getUser().getEmail() : "anonymous";
            NotificationDto notif = NotificationDto.builder()
                    .recipient(recipientEmail)
                    .title("Order Status Updated")
                    .message("Order #" + updatedOrder.getOrderNumber() + " is now " + newStatus)
                    .type(NotificationType.ORDER)
                    .timestamp(LocalDateTime.now())
                    .build();

            notificationService.broadcast(notif);
        } catch (Exception e) {
            log.warn("Failed to dispatch WebSocket notification for order status update: {}", e.getMessage());
        }

        return orderMapper.toDto(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDto cancelOrder(Long id, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (userId != null && order.getUser() != null && !order.getUser().getId().equals(userId)) {
            throw new BusinessException("You are not authorized to cancel this order", "UNAUTHORIZED", HttpStatus.FORBIDDEN);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Order is already cancelled", "ALREADY_CANCELLED", HttpStatus.BAD_REQUEST);
        }

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("Cannot cancel order in status: " + order.getStatus(), "CANNOT_CANCEL", HttpStatus.BAD_REQUEST);
        }

        restoreStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);

        try {
            NotificationDto notif = NotificationDto.builder()
                    .recipient(order.getUser() != null ? order.getUser().getEmail() : "user")
                    .title("Order Cancelled")
                    .message("Order #" + updatedOrder.getOrderNumber() + " has been cancelled.")
                    .type(NotificationType.WARNING)
                    .timestamp(LocalDateTime.now())
                    .build();
            notificationService.broadcast(notif);
        } catch (Exception e) {
            log.warn("Failed to dispatch WebSocket notification for cancelled order: {}", e.getMessage());
        }

        return orderMapper.toDto(updatedOrder);
    }

    @Override
    public OrderSummaryStatsDto getOrderStats() {
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long confirmedOrders = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long shippedOrders = orderRepository.countByStatus(OrderStatus.SHIPPED);
        long deliveredOrders = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();

        return OrderSummaryStatsDto.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .confirmedOrders(confirmedOrders)
                .shippedOrders(shippedOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .build();
    }

    @Override
    public List<OrderResponseDto> searchOrders(String keyword) {
        return orderRepository.searchOrders(keyword)
                .stream()
                .map(orderMapper::toDto)
                .toList();
    }

    private void restoreStock(Order order) {
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
                    log.info("Restored stock of product '{}' to {}", product.getName(), restored);
                }
            }
        }
    }

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "ORD-" + datePart + "-" + randomPart;
    }
}
