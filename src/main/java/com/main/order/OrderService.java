package com.main.order;

import java.util.List;

public interface OrderService {

    OrderResponseDto createOrder(OrderCreateRequestDto request);

    List<OrderResponseDto> getAllOrders();

    List<OrderResponseDto> getOrdersByUserId(Long userId);

    OrderResponseDto getOrderById(Long id);

    OrderResponseDto updateOrderStatus(Long id, OrderStatusUpdateRequestDto dto);

    OrderResponseDto cancelOrder(Long id, Long userId);

    OrderSummaryStatsDto getOrderStats();

    List<OrderResponseDto> searchOrders(String keyword);
}
