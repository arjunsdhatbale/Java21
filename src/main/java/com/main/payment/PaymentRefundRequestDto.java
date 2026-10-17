package com.main.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefundRequestDto {

    @NotBlank(message = "Reason is required for refund")
    private String reason;
}
