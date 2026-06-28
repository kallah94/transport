package com.gayale.transport.dto.payment;

import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "La date de debut de validite est obligatoire")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private Boolean active = true;
}
