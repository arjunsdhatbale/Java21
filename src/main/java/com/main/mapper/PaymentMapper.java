package com.main.mapper;

import com.main.model.dto.PaymentResponseDto;
import com.main.model.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponseDto toDto(Payment payment) {
        if (payment == null) {
            return null;
        }

        String customerName = "Unknown";
        String customerEmail = "";
        Long userId = null;
        Long orderId = null;
        String orderNumber = "";

        if (payment.getUser() != null) {
            userId = payment.getUser().getId();
            customerName = ((payment.getUser().getFirstName() != null ? payment.getUser().getFirstName() : "")
                    + " "
                    + (payment.getUser().getLastName() != null ? payment.getUser().getLastName() : "")).trim();
            customerEmail = payment.getUser().getEmail() != null ? payment.getUser().getEmail() : "";
        }

        if (payment.getOrder() != null) {
            orderId = payment.getOrder().getId();
            orderNumber = payment.getOrder().getOrderNumber() != null ? payment.getOrder().getOrderNumber() : "";
        }

        return PaymentResponseDto.builder()
                .id(payment.getId())
                .transactionId(payment.getTransactionId())
                .orderId(orderId)
                .orderNumber(orderNumber)
                .userId(userId)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .amount(payment.getAmount())
                .currency(payment.getCurrency() != null ? payment.getCurrency() : "INR")
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "")
                .paymentStatus(payment.getPaymentStatus() != null ? payment.getPaymentStatus().name() : "")
                .gatewayReference(payment.getGatewayReference())
                .failureReason(payment.getFailureReason())
                .paymentDate(payment.getPaymentDate())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
