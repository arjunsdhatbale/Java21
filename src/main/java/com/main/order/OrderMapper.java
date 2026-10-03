package com.main.order;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class OrderMapper {

    public OrderItemResponseDto toItemDto(OrderItem item) {
        if (item == null) {
            return null;
        }
        return OrderItemResponseDto.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProductName())
                .productImageUrl(item.getProduct() != null ? item.getProduct().getImageUrl() : null)
                .productPrice(item.getProductPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }

    public OrderResponseDto toDto(Order order) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponseDto> itemDtos = order.getItems() != null
                ? order.getItems().stream().map(this::toItemDto).toList()
                : Collections.emptyList();

        int totalItemsCount = order.getItems() != null
                ? order.getItems().stream().mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0).sum()
                : 0;

        String customerName = "Unknown";
        String customerEmail = "";
        String customerPhone = "";
        Long userId = null;

        if (order.getUser() != null) {
            userId = order.getUser().getId();
            customerName = (order.getUser().getFirstName() != null ? order.getUser().getFirstName() : "")
                    + " "
                    + (order.getUser().getLastName() != null ? order.getUser().getLastName() : "");
            customerName = customerName.trim();
            customerEmail = order.getUser().getEmail() != null ? order.getUser().getEmail() : "";
            customerPhone = order.getUser().getPhone() != null ? order.getUser().getPhone() : "";
        }

        return OrderResponseDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(userId)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .customerPhone(customerPhone)
                .status(order.getStatus() != null ? order.getStatus().name() : "PENDING")
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .contactPhone(order.getContactPhone())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "PENDING")
                .notes(order.getNotes())
                .totalItems(totalItemsCount)
                .items(itemDtos)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
