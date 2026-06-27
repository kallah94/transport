package com.gayale.transport.dto.payment;

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
public class TransporterRateResponse {

    private String id;
    private String transporterId;
    private double pricePerTonne;
    private String projectId;
    private String product;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
