package com.gayale.transport.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverPayoutLine {
    private String vehicle;
    private String driver;
    private int tripCount;
    private double totalTonnes;
    private double pricePerTonne;
    private double grossAmount;
    private double fuelLitres;
    private double fuelAmount;
    private double netAmount;
}
