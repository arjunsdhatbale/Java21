package com.main.model.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatsDto {

    private long totalTransactions;
    private long successfulTransactions;
    private long pendingTransactions;
    private long failedTransactions;
    private long refundedTransactions;
    private BigDecimal totalVolume;
}
