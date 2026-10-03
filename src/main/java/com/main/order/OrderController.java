package com.main.order;

import com.main.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
            @Valid @RequestBody OrderCreateRequestDto request) {
        logger.info("Request received to place a new order for userId: {}", request.getUserId());
        OrderResponseDto response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getAllOrders(
            @RequestParam(required = false) Long userId) {
        logger.info("Request received to get orders (filter userId: {})", userId);
        List<OrderResponseDto> orders = (userId != null)
                ? orderService.getOrdersByUserId(userId)
                : orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.success("Orders fetched successfully", orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrderById(@PathVariable Long id) {
        logger.info("Request received to get order by id: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Order fetched successfully", orderService.getOrderById(id)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getOrdersByUserId(@PathVariable Long userId) {
        logger.info("Request received to get orders for user: {}", userId);
        return ResponseEntity.ok(ApiResponse.success("User orders fetched successfully", orderService.getOrdersByUserId(userId)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponseDto>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequestDto dto) {
        logger.info("Request received to update order status for id: {} to {}", id, dto.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", orderService.updateOrderStatus(id, dto)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponseDto>> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        logger.info("Request received to cancel order id: {} by userId: {}", id, userId);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", orderService.cancelOrder(id, userId)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<OrderSummaryStatsDto>> getOrderStats() {
        logger.info("Request received to get order summary stats");
        return ResponseEntity.ok(ApiResponse.success("Order statistics fetched successfully", orderService.getOrderStats()));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> searchOrders(@RequestParam String keyword) {
        logger.info("Request received to search orders with keyword: {}", keyword);
        return ResponseEntity.ok(ApiResponse.success("Order search results", orderService.searchOrders(keyword)));
    }
}
