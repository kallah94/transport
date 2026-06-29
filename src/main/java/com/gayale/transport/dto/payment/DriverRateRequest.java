package com.gayale.transport.dto.payment;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverRateRequest {

    @Positive(message = "Le tarif chauffeur (XOF/tonne) doit etre positif")
    private double pricePerTonne;

    private String transporterId;

    // Optionnelle : si absente, le tarif est valable depuis toujours
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private Boolean active = true;
}
