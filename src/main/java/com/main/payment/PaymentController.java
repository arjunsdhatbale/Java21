package com.main.payment;

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
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> processPayment(
            @Valid @RequestBody PaymentProcessRequestDto request) {
        logger.info("Request received to process payment for orderId: {}, method: {}",
                request.getOrderId(), request.getPaymentMethod());
        PaymentResponseDto response = paymentService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment processed successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponseDto>>> getAllPayments(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long orderId) {
        logger.info("Request received to get payments (filter userId: {}, orderId: {})", userId, orderId);
        List<PaymentResponseDto> payments = paymentService.getAllPayments(userId, orderId);
        return ResponseEntity.ok(ApiResponse.success("Payments fetched successfully", payments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getPaymentById(@PathVariable Long id) {
        logger.info("Request received to get payment by id: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Payment fetched successfully", paymentService.getPaymentById(id)));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getPaymentByOrderId(@PathVariable Long orderId) {
        logger.info("Request received to get payment for order id: {}", orderId);
        return ResponseEntity.ok(ApiResponse.success("Order payment fetched successfully", paymentService.getPaymentByOrderId(orderId)));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> refundPayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRefundRequestDto request) {
        logger.info("Request received to refund payment id: {}", id);
        PaymentResponseDto response = paymentService.refundPayment(id, request);
        return ResponseEntity.ok(ApiResponse.success("Payment refunded successfully", response));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PaymentStatsDto>> getPaymentStats() {
        logger.info("Request received to get payment statistics");
        return ResponseEntity.ok(ApiResponse.success("Payment statistics fetched successfully", paymentService.getPaymentStats()));
    }
}
