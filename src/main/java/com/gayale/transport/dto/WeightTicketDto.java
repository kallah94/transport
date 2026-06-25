package com.gayale.transport.dto;

import com.gayale.transport.model.WeightTicket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WeightTicketDto {

    private String id;

    private String ticketNumber;

    @NotNull(message = "Date is required")
    private LocalDate date;

    // @NotNull est sans effet sur un primitif (jamais null) : on garde uniquement @Positive
    @Positive(message = "Empty weight must be positive")
    private double emptyWeight;

    @Positive(message = "Loaded weight must be positive")
    private double loadedWeight;

    private double netWeight;

    private double variance;

    @NotBlank(message = "Vehicle is required")
    private String vehicle;

    @NotBlank(message = "Driver is required")
    private String driver;

    @NotBlank(message = "Product is required")
    private String product;

    @NotBlank(message = "Client is required")
    private String client;

    @NotBlank(message = "Supplier is required")
    private String supplier;

    private String origin;

    @NotBlank(message = "Destination is required")
    private String destination;

    @NotBlank(message = "Transporter is required")
    private String transporter;

    @NotBlank(message = "Purchase order number is required")
    private String purchaseOrderNumber;

    @NotBlank(message = "Project ID is required")
    private String projectId;

    @NotBlank(message = "Purchase order ID is required")
    private String purchaseOrderId;

    @NotBlank(message = "Operator name is required")
    private String operatorName;

    private String qrCode;

    private WeightTicket.TicketStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
