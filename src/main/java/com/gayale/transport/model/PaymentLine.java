package com.gayale.transport.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentLine {
    private String ticketId;
    private String ticketNumber;
    private LocalDate date;
    private String vehicle;
    private String driver;
    private double tonnes;
    // Facture entreprise -> transporteur
    private double transportAmount;
    private double distanceKm;
    private double fuelLitres;
    private double fuelAmount;
    private String note;
}
