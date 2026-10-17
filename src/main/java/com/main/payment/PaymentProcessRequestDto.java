package com.main.payment;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessRequestDto {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Payment method is required")
    private String paymentMethod;

    // Optional simulated payment details
    private String upiId;
    private String cardNumber;
    private String cardHolder;
    private String cardExpiry;
    private String cvv;
    private String bankName;
    private String notes;
}
