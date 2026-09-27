package com.main.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateRequestDto {

    @NotBlank(message = "Status is required")
    private String status;
}
