package com.gayale.transport.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverPaymentLine {
    private String driver;
    private int tripCount;
    private double tonnes;
    private double grossAmount;   // tonnes x tarif chauffeur
    private double fuelDeducted;  // carburant defalque
    private double netAmount;     // gross - fuel
}
