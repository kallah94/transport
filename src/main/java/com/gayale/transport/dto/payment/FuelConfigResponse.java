package com.gayale.transport.dto.payment;

import com.gayale.transport.model.FuelConfig;
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
public class FuelConfigResponse {

    private String id;
    private double fuelPricePerLitre;
    private double roundTripFactor;
    private FuelConfig.DotationMode dotationMode;
    private LocalDate effectiveFrom;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
