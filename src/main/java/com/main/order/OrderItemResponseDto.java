package com.main.order;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDto {

    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
