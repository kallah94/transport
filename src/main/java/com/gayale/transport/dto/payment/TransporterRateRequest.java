package com.gayale.transport.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransporterRateRequest {

    @NotBlank(message = "L'ID du transporteur est obligatoire")
    private String transporterId;

    @Positive(message = "Le tarif (XOF/tonne) doit etre positif")
    private double pricePerTonne;

    private String projectId;

    private String product;

    @NotNull(message = "La date de debut de validite est obligatoire")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private Boolean active = true;
}
