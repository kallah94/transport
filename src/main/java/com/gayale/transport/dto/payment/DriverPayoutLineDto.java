package com.gayale.transport.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverPayoutLineDto {
    private String vehicle;
    private String driver;
    private int tripCount;
    private double totalTonnes;
    private double pricePerTonne;   // tarif chauffeur (XOF/tonne)
    private double grossAmount;     // tonnage x prix/tonne
    private double fuelLitres;      // dotation carburant (litres)
    private double fuelAmount;      // dotation carburant (XOF)
    private double netAmount;       // gross - carburant
}
