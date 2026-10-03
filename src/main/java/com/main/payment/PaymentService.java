package com.main.payment;

import java.util.List;

public interface PaymentService {

    PaymentResponseDto processPayment(PaymentProcessRequestDto request);

    PaymentResponseDto getPaymentById(Long id);

    PaymentResponseDto getPaymentByOrderId(Long orderId);

    List<PaymentResponseDto> getAllPayments(Long userId, Long orderId);

    PaymentResponseDto refundPayment(Long paymentId, PaymentRefundRequestDto request);

    PaymentStatsDto getPaymentStats();
}
