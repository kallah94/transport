package com.gayale.transport.dto.payment;

import com.gayale.transport.model.FuelConfig;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FuelConfigRequest {

    @Positive(message = "Le prix du carburant (XOF/L) doit etre positif")
    private double fuelPricePerLitre;

    @Positive(message = "Le facteur aller-retour doit etre positif")
    private double roundTripFactor;

    // Optionnel : champ conserve pour compatibilite, non utilise dans le calcul
    private FuelConfig.DotationMode dotationMode;

    private LocalDate effectiveFrom;
}
