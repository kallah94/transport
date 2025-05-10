package com.gayale.transport.dto;

import com.gayale.transport.model.PurchaseOrder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrderDto {

    private String id;

    @NotBlank(message = "Order number is required")
    private String orderNumber;

    @NotBlank(message = "Supplier is required")
    private String supplier;

    @NotBlank(message = "Transporter is required")
    private String transporter;

    @NotNull(message = "Ordered quantity is required")
    @Positive(message = "Ordered quantity must be positive")
    private double orderedQuantity;

    private double deliveredQuantity;

    private double remainingQuantity;

    private double deliveryPercentage;

    @NotBlank(message = "Project ID is required")
    private String projectId;

    private PurchaseOrder.OrderStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

